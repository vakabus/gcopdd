# Blood

## Project goals

Idea behind Blood is to inject data collection tools into Graal Compiler. These tools will then monitor the compiler during it's runtime and collect interesting data that will be dumped to disk. It's expected, that this tool will slow the compiler a bit and it can change it's behaviour a bit. This needs to be considered when analysing the data.

## Collection metodology

All data are collected per compilation unit. In practise it means that data are collected for each method, for each HotSpot snippet and also all OSR compilations. Aggregation of all the data can be done in post-processing.

All results are accumulated in `dumps` directory located in the working directory of running application. In there, there are directories - one for every run. In them all the data are stored in a series of files.

### Data files naming and format

All files are named in similar fashion. Their name starts with an opaque alphanumeric identifier and it is followed by an extension specifying the tool which created the file. There are two special file types - `.request` and `.timing`. `.request` file type contains compilation request identifier, which could be presented to the user. `.timing` file type contains information about when compilation occured and how long it took, so that it could be used for sorting.

Format is always plain text in UTF-8 encoding. The files can also be compressed using gzip. In that case their filename will be extended with extension `.gz`. Compression is on by default and it's configurable only through code. It can be made configurable via some other method if there is a demand.

## Collection tools

Generally, when there is a matrix, it will contain values in format `X:Y:Z`. `X` is number of positive occurences of some event, `Y` is number of all occurences of the event and `Z` is number of times it was measured.

### Phase stack (`.phasestack`)

As described [here](graal_internals.md), optimization phases are grouped into phase suites and they call each other it a shallow recursive pattern. This tool monitors this and traces which optimization phases were called and when. It outputs a list of phases as they were active.

## Phase dependency matrix (`.depmat`)

Tracks in which phases were graph nodes created. Then for each phase we get an information from where are the nodes coming so that we can analyse dependency of one phase another one.

The file contains first a list of phase numbers referencing phases in `.phasestack`. This list is then used as a legend for the matrix values - upper left corner contains information about nodes that were creates in the first phase and that entered the first phase in the list. Rows of the matrix represent the actual phases, columns represent the origin of the nodes.

## Node type list (`.nodelist`)

Collects a list of all node types seen during this compilation unit. Outputs just a simple text file with a list of Java classes.

## Node type tracker (`.nodemat`)

Tracks types of nodes across optimization phases. For every phase, we want to know, which types of nodes were there when the phase started and exited. And not only types but also number of nodes.

The file contains two matrices, one with information about number and types of nodes entering phases, the other about nodes leaving phases. It starts with two lists separeted by an empty line. The first one is information about rows - it references phases in `.phasestack`. The second list contains types of nodes used, represented by the name of their Java class. Then the entry matrix follow, after that the out-of-phase matrix.

## Compilation event information (`.request`)

Contains serialized compilation request from Graal. It's possible to identify OSR, Stub or normal compilation from this, but beware, that the textual representation might change. It's not under our control.

## Timing information (`.timing`)

Contains compilation start time and it's duration.
