# --------------------------------------------
# TemplateCreator
#
# Generates DynamicComponents-AI2 schemas by 
# parsing App Inventor project file automatically.
#
# - Yusuf Cihan
#
# MIT license.
# --------------------------------------------

import ast
import re
import json

EXTENSIONS = {}
KEYS = []

# Color converter for AI2
def BuildColor(code : str):
    A = 255
    val = str(code)[2:]
    if len(val) != 6:
        A = int(str(val)[0:2], 16)
    R = int(str(val)[2:4], 16)
    G = int(str(val)[4:6], 16)
    B = int(str(val)[6:], 16)
    if A == 0:
        return 255
    else:
        return (B + (G + (R + (256 * A)) * 256) * 256) - 4294967296


def Rearrange(obj : dict):
    global EXTENSIONS
    global KEYS
    for key, value in obj.copy().items():
        if key in obj:
            # If key ends with Uuid or Version, ignore it.
            # Because DynamicComponents-AI2 extension's JSON templates doesn't need it.
            if key == "Uuid" or key == "$Version":
                del obj[key]
            # Rename the $Name according to the template structure.
            elif key == "$Name":
                obj["id"] = value
                del obj[key]
            # Rename the $Type according to the template structure.
            elif key == "$Type":
                # Use the extension's full class name if it is defined in the extensions dictionary.
                if value in EXTENSIONS:
                    obj["type"] = EXTENSIONS[value] 
                    del obj[key]
                else:
                    obj["type"] = value
                    del obj[key]
            # Rename the $Components according to the template structure.
            elif key == "$Components":
                obj["components"] = value
                del obj[key]
            # Move the properties inside a "properties" object.
            # components/Button/Text --> components/Button/properties/Text
            else:
                # Copy of the value for editing purposes.
                v = value
                # Create a empty dictionary named properties if doesn't exists.
                if "properties" not in obj:
                    obj["properties"] = {}
                # Convert value to color if it presents a color code.
                # Colors are started with &H.
                if str(value).startswith("&H") and (len(str(value)) == 8 or len(str(value)) == 10):
                    v = BuildColor(v)
                # Parse the values to their type automatically.
                # If any error raised, use the value without changing the type.
                try:
                    x = ast.literal_eval(v)
                    obj["properties"][key] = x
                except:
                    obj["properties"][key] = v
                del obj[key]
            # If value contains template parameters, add them to the KEYS list.
            if value is str:
                for parameter in re.findall(r'(?<=(?<!\{)\{)[^{}]*(?=\}(?!\}))', value):
                    if parameter not in KEYS:
                        KEYS.append(parameter)
    return obj


def GenerateTemplate(SCM : dict, extensions : dict):
    # Template that will be modified later.
    template = {
        # Use app name as template name.
        "name": SCM["Properties"]["AppName"],
        # Current metadata version. 
        # Needs to be 1, until a new type of metadata releases.
        "metadata-version": 1,
        # Extension version that this template generated for.
        "extension_version": 5,
        # Template author name.
        "author": "<your name>",
        # List of AI2 distributions that will template work on.
        "platforms": SCM["authURL"],
        # Contains used extensions in this template along with their class names.
        # Example:
        # {
        #   "HelloWorld": "io.foo.HelloWorld"
        # }
        "extensions": extensions,
        # Template parameters.
        # Will be generated automatically from SCM.
        "keys": [],
        # Components that will be created.
        # Will be generated automatically from SCM.
        "components": []
    }
    global EXTENSIONS
    global KEYS
    # Save extensions in the list.
    EXTENSIONS = extensions
    # Convert SCM data to Dynamic Components schema data.
    template["components"] = json.loads(json.dumps(SCM), object_hook = Rearrange)["properties"]["Properties"]["components"]
    # Save found keys in the template.
    template["keys"] = KEYS
    # Remove DynamicComponent instances from template, because they are not needed.
    for component in template["components"].copy():
        if component["type"] == "DynamicComponents":
            if component in template["components"]:
                template["components"].remove(component)

    return template