from guizero import App, Window, Text, ButtonGroup, PushButton
import sys
import zipfile
import json
import os
from pathlib import Path

try:
    from TemplateCreate import GenerateTemplate
except:
    from TemplateCreator.TemplateCreate import GenerateTemplate

app = App(title="TemplateCreator")
app.visible = False

SCREENS = {}
PROJECTS = {}
EXTENSIONS = {}

# ----------------------------
#  Generate()
#
#  Generates the template
#  and saves in the file.
# ----------------------------
def Generate(screenName):
    app.visible = False
    # Check if screenName is set correctly and exists in the vairables.
    if (screenName not in SCREENS) or (screenName not in PROJECTS):
        app.error("Error", "We couldn't found that screen in the project.")
        app.destroy()
        sys.exit(1)
    # Otherwise, create the project.
    else:
        template = GenerateTemplate(PROJECTS[screenName], EXTENSIONS)
        open(template["name"] + " - " + screenName + ".json", "w+").write(json.dumps(template, indent = 4))
        app.info("Completed", "Done!")
        app.destroy()
        sys.exit(0)

def main():
    # Show a open file dialog to select an project from computer.
    file = app.select_file(filetypes=[["App Inventor Project File", "*.aia"], ["App Inventor Screen File", "*.ais"]])
    # Check if user canceled the dialog.
    if str(file).strip() == "":
        sys.exit()
    # If the project file is invalid, show an error message.
    elif not os.path.exists(file):
        app.error("Error", "This project doesn't exists!")
        sys.exit(1)
    else:
        try:
            # Read the AIA.
            with zipfile.ZipFile(file, "r") as z:
                global SCREENS
                global PROJECTS
                global EXTENSIONS
                # Get the screen names.
                SCREENS = {name.split("/")[-1].replace(".scm", "") : name for name in z.namelist() if name.startswith("src/") and name.endswith(".scm")}
                # Get the extension names and their class names by reading their components.json files.
                # Save the extension name and type in the dictionary.
                for extension in [name for name in z.namelist() if name.startswith("assets/external_comps/") and name.endswith("/components.json")]:
                    componentDetails = json.loads(z.open(extension, "r").read())[0]
                    EXTENSIONS[componentDetails["name"]] = componentDetails["type"]
                # Read the screens' SCM files.
                # Then, save the results in the dictionary.
                for screenName, screenPath in SCREENS.items():
                    f = z.open(screenPath, "r").readlines()[2].decode(sys.stdout.encoding)
                    PROJECTS[screenName] = json.loads(f) 
                # If no screen has found, exit from app.
                if not SCREENS:
                    app.error("Error", "This project doesn't contains any screens!")
                    sys.exit(1)
                # If there are more than 1 screens, show a screen selecting window
                # with a button group.
                elif len(SCREENS) > 1:
                    text = Text(app, text = "Select a screen for generating template:")
                    choice = ButtonGroup(app, options = list(SCREENS.keys()))
                    accept = PushButton(app, width = "fill", align = "bottom", text = "Generate", command = lambda: Generate(choice.value))
                    app.visible = True
                    app.display()
                # If the project only contains 1 screen, create the template.
                else:
                    Generate(list(SCREENS.keys())[0])
        # Show an error message if the ZIP file is corrupted.
        except zipfile.BadZipFile:
            app.error("Error", "This file is not a valid App Inventor project!")
            sys.exit(1)

if __name__ == "__main__":
    main()