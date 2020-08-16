# TemplateCreator
# by Yusuf Cihan

import json
import ast
from flatten_json import flatten, unflatten_list
import re

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

    # Create a variable to store modified flatted JSON.
    flatten_json = {}

    # Edit the flatten JSON.
    for key, value in flatten(SCM["Properties"], "/").items():
        k = str(key)
        val = value
        # If key ends with Uuid or Version, ignore it.
        # Because DynamicComponents-AI2 extension's JSON templates doesn't need it.
        if k.endswith("/Uuid") or k.endswith("/$Version"):
            continue
        # Else;
        else:
            # Replace the "$Components" with "components" according to the template structure.
            # $Components --> components
            k = k.replace("/$Components/", "/components/")

            # Rename the $Name and $Type according to the template structure.
            # $Name --> id
            # $Type --> type
            if k[-5:] in ["$Name", "$Type"]:
                k = k.replace("/$Name", "/id").replace("/$Type", "/type")
            # Move the properties inside a "properties" object.
            # components/Button/Text --> components/Button/properties/Text
            else:
                path = k.split("/")
                path.insert(-1, "properties")
                k = "/".join(path)

            # Check if value contains template parameter(s).
            # Parameters are defined with curly brackets.
            # {text}, {age}, {color}
            for parameter in re.findall(r'(?<=(?<!\{)\{)[^{}]*(?=\}(?!\}))', str(value) + " " + k):
                if parameter not in template["keys"]:
                    template["keys"].append(parameter)

            # Try to convert the value automatically.
            # So if value is "True" or "False", then it will be converted to the bool and so on.
            try:
                val = ast.literal_eval(value)
            except:
                pass

            # An exception for the color converting.
            if str(val).startswith("&H"):
                if len(str(val)[2:]) == 6:
                    val = "&HFF" + str(val)[2:] 
                val = str(val)[2:]
                A = int(str(val)[0:2], 16)
                R = int(str(val)[2:4], 16)
                G = int(str(val)[4:6], 16)
                B = int(str(val)[6:], 16)
                val = (B + (G + (R + (256 * A)) * 256) * 256) - 4294967296

            # If the component name is in the extensions list,
            # then use its full internal name as it is an external package that
            # doesn't exists in the App Inventor sources.
            if k.endswith("/type") and (val in extensions):
                val = extensions[val]

            # Add the value and key to the modified flatten dictionary.
            flatten_json[k] = val

    # Now, unflat the modified flatten dictionary.
    # Save the output to the template.
    template["components"] = unflatten_list(flatten_json, "/")["$Components"]

    # Remove DynamicComponent instances from template, because it is not needed.
    for component in template["components"].copy():
        if component["type"] == "DynamicComponents":
            if component in template["components"]:
                template["components"].remove(component)

    # Return the template.
    return template