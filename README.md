This is a simple tool to analyze the dependency of the project.

## Install the graph viz
We need to install the graphviz to generate the graph.
```bash
brew install graphviz
```
## Install the kotlingraph
Download the most recent version of kotlingraph from the [github release](https://github.com/YoungMoon-DoorDash/kotlingraph/tree/master/release).
 - unzip the downloaded file.
 - move the unzipped content to the proper location.
 - Be sure to add the kotlingraph to the path.

## Usages

To build a dependency graph for a class, we need to run the following command:

```bash
kotlingrah -r <project src folder> -c <class name to build depency graph>
```

For example, the following command will create a dependency graph for the ```GetDashPassCampaignsGrcController```:
- Generate a SVG file with the class name under the current folder.
- Open the SVG file with the default browser.

```bash
kotlingraph -r /Users/youngmoonko/Projects/consumer-service/src/main -c GetDashPassCampaignsGrpcController
```

### Shell command in the Mac
You can set the following command in your ```.bash_profile``` to run the command more easier:
- Open the ```.bash_profile``` file using an editor.
- Copy and put the below code into your ```.bash_profile``` file.
- Replace the kotlingraph installation path and project root path with your local configuration.

```bash
## Run kotlin graph
## Usage: kotlingraph <name of class to build graph>
kotlingraph() {
    /Users/youngmoonko/Applications/kotlingraph/bin/kotlingraph -r /Users/youngmoonko/Projects/consumer-service/src/main/kotlin -c $1
}
```

Now, you can run the command in the terminal as simply by just proving the class name to build dependency graph like this:
- Be sure to open a new terminal to apply changed bash profile.
```bash
kotlingraph GetDashPassCampaignsGrpcController
```
### Sample graph
- Each class prefixed with high-level package name.
- Each class within the same package is colored with same color.
- Tool will generate two graphs:
   - Firt one shows all used class dependency by the specified class.
![GetDashPassCampaignsGrpcController](https://github.com/YoungMoon-DoorDash/kotlingraph/assets/122405315/098b9643-8e2c-421a-8cd6-1a93e7c83550)
   - Second one postfixed with "_dep" shows all classes using the specified class. 
![SubscriptionHelper_dep](https://github.com/YoungMoon-DoorDash/kotlingraph/assets/122405315/340ca300-edfb-448f-baec-577dd980a156)

