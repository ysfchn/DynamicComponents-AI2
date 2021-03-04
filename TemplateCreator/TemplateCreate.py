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
def create_color(hex_code):
    length = len(hex_code)
    starts_with_format = hex_code.startswith("&H")

    if starts_with_format and (length == 10):
        # Split everything after the '&H' and update the length
        hex_code = hex_code[2:]
        length = len(hex_code)
    else:
        if not length == 10 and not starts_with_format:
            # Criteria has not been met
            return 'Failed to parse hex code.'
        if not length == 10:
            # Hex code is not 10 characters in length
            return 'The hex code was not the correct length.'
        if not starts_with_format:
            # Hex code does not start with '&H
            return 'The hex code is incorrectly formatted.'
        else:
            # This shouldn't happen
            return 'Unknown error occurred.'

    if length == 8:
        try:
            alpha = int(hex_code[0:2], 16)
            red = int(hex_code[2:4], 16)
            green = int(hex_code[4:6], 16)
            blue = int(hex_code[6:8], 16)
        except ValueError:
            return f'The hex code contains unknown values.'
        else:
            return (alpha & 0xff) << 24 | (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff)
    else:
        # This shouldn't happen
        return f'"{hex_code}" is not a valid hex code.'


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
                    v = create_color(v)
                # Parse the values to their type automatically.
                # If any error raised, use the value without changing the type.
                try:
                    x = ast.literal_eval(v)
                    if key == "Text":
                        obj["properties"][key] = str(v)
                    else:
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
