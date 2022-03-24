# Palladio-Addons-PlantUML

[![MavenVerify](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/maven-verify.yml) [![CodeQL](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/codeql-analysis.yml) [![Dokumentation](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/build_latex.yml/badge.svg)](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/build_latex.yml) [![GitHubPages](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/pages/pages-build-deployment/badge.svg)](https://github.com/PalladioSimulator/Palladio-Addons-PlantUML/actions/workflows/pages/pages-build-deployment)

### Objectives: 
- To gain better understanding of the system by visualisation
- To provide fast and parallel view (UML Diagram) next to the tree-/code-editor in Eclipse
- To replace the existing PlantUML diagram by a new simplified one
- Transformation of elements from Palladio models to UML elements programatically

[PlantUML View in Eclipse](./presentation/plantuml+repo.png)

### Foundations:
#### The Palladio Component Model (PCM) 
is designed to early analyze software qualities like performance, reliability, maintainability, and cost predictions for software architectures. The PCM is implemented using the Eclipse Modeling Framework (EMF).
A software architecture is specified with respect to static structure, behaviour, deployment/allocation, resource environment/execution environment, and usage profile. In the PCM, software is described in terms of components, connectors, interfaces, individual service behaviour models (so-called Service Effect Specifications, SEFF), servers, middleware, virtual machines, network, the allocation of components and servers, models of the user interaction with the system, etc. Overall, the PCM captures multiple models of software systems, as follows:
- Repository model
- System model
- Allocation model
- Usage model - not relevant for this work

#### PlantUML
is a tool that allows to quickly write UML Diagrams (class diagram, sequence diagram, usecase diagram, object diagram...) and also some other types of diagrams (JSON data, YAML data, Gantt diagram...). Diagrams are defined by a specific textual language, which is very intuitive and straight-forward for the user. The language is unambiguous and the order of elements in the text does not represent their order of appearance in the diagram. Although PlantUML auto layout has some shortcomings, usually the diagrams are displayed in an optimal look. Images can be generated in PNG, in SVG or in LaTeX format. It is also possible to generate ASCII art diagrams (only for sequence diagrams)
The tool offers an [online editor](http://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa70000 "online editor") and a plugin for Eclipse. It is also possible to generate diagrams from the command line.
This is an [example](./presentation/plantuml_beispiel.png) of a transformation.

#### Eclipse Plugin
Since the PlantUML tool is structured in plugins, it was reasonable to extend it by creating a new plugin project in Eclipse. The integration of new code was problem-free and caused no changes to the existing code.

### Architecture:

The new bundle contains one class per diagram type that is considered. As the [diagram](./presentation/klassendiagramm-eng.drawio.pdf) shows, we have - `PcmComponentDiagramIntent, PcmSystemDiagramIntent and PcmAllocationDiagramIntent`. In these classes the Palladio elements are mapped to the textual notation of PlantUML. The actual display of diagrams was already implemented.
PlantUML generally functions by deciding, which `DiagramTextProvider` carries out the transformation. For this, PlantUML makes a query to every `DiagramTextProvider`. The query contains the selected editor and the open view. Then the available providers give information whether they support this Eclipse configuration. The `DiagramTextProvider` are sorted by priority and the first one to meet both criteria carries out the transformation. So in order to "offer" a new transformation, we implemented `PcmDiagramIntentProvider`. 
By calling the method `getDiagramInfos()` the provider adds the new diagram, depending on the type of `EObject` (`Repository, System, Allocation`).

### Transformations:
For all models we used the MediaStore example.
#### The Repository model
is transformed by iterating through the components and displaying their provided/required interfaces. If a component does not provide/require an interface, it is included separately in the diagram. This is an [example](./presentation/repository.png) of the generated diagram and the source code: 
```markdown
@startuml
skinparam fixCircleLabelOverlapping true
IDownload-[EnqueueDownloadCache]
[EnqueueDownloadCache]..>IDownload : requires
IDownload-[InstantDownloadCache]
[InstantDownloadCache]..>IDownload : requires
IFileStorage-[FileStorage]
IMediaManagement-[MediaManagement]
[MediaManagement]..>IDownload : requires
[MediaManagement]..>IPackaging : requires
[MediaManagement]..>IMediaAccess : requires
IPackaging-[Packaging]
IMediaAccess-[MediaAccess]
IDownload-[MediaAccess]
[MediaAccess]..>IFileStorage : requires
@enduml
```
#### The System model
is transformed by first appending the provided interface (by the whole system). Then a new component is added (usually called "System"). After that, all the connectors are included. If an assembly context does not participate in a connection, it is added separately at the end. For easy navigation, the repository is linked in the system diagram. This is an [example](./presentation/system.png) of the generated diagram.

#### The Allocation model
differs from the system model just by the containers. They encapsulate allocation contexts. An allocation conext cannot exist outside of a container. This is an [example](./presentation/allocation.png) of the generated diagram.

### Limitations:
- Diagrams are not interactive. This means they cannot be edited in the graphic editor (e.g. by "drag-and-drop"). This is because they are generated as a PNG image. Nevertheless, diagrams could be helpful in failure analysis.
- Strong dependency on the PlantUML layout algorithms. Sometimes they are not as precise as a person, for example there could be some overlapping labels of the connections. Improving these algorithms could be a subject to future work.

