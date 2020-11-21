# ⚡ Template Creator <small>for Dynamic Components AI2</small>
This sub-project allows you to create templates from App Inventor Project files automatically! So you don't need to write templates manually anymore!

It includes two scripts. `TemplateCreate.py` does the main job which is generating the template, and `menu.py` is made for you to access the `TemplateCreate.py` easily.

> This script requires Python that needs to be installed, but if you have a solution that will work on everyone's computer without installing something, you can always create a Pull Request and a new tool for that :)

To get started, let's make sure everything is ready!

## 🚧 Requirements
* **Python 3.x**<br>
Select "Add to the PATH" option during setup.

Then install the external modules by executing `pip install -r requirements.txt` in this directory.

## 📦 Usage

* Execute the `main.py` file. 
  You can do that in your terminal by entering this folder and executing `python main.py`.  

* A file dialog will open, just select the AIA/AIS file that you want to convert, then click "Open" button.

* If the project contains more than one screen, you will be asked to select which screen will be used with selection dialog.

If everything goes well, you will see the generated JSON file in this directory.

## 🏅 License

Source code is licensed under MIT license. You must include the license notice in all copies or substantial uses of the work.

---

If you encounter any issues with script, just create a new issue.