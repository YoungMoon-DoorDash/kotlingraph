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
