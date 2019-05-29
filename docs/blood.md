# Blood

## Project goals

Idea behind Blood is to inject data collection tools into Graal Compiler. These tools will then monitor the compiler during it's runtime and collect interesting data that will be dumped to disk. It's expected, that this tool will slow the compiler a bit and it can change it's behaviour a bit. This needs to be considered when analysing the data.

## Collection metodology

All data are collected per compilation unit. In practise it means that data are collected for each method, for each HotSpot snippet and also all OSR compilations. Aggregation of all the data can be done in post-processing.

All results are accumulated in `dumps` directory located in the working directory of running application. In there, there are directories - one for every run. In them all the data are stored in a series of files.

### Data files naming and format

All files are named in similar fashion. Their name starts with an opaque alphanumeric identifier and it is followed by an extension specifying the tool which created the file. There are two special file types - `.request` and `.timing`. `.request` file type contains compilation request identifier, which could be presented to the user. `.timing` file type contains information about when compilation occured and how long it took, so that it could be used for sorting.

Format is always plain text in UTF-8 encoding. The files can also be compressed using gzip. Then their filename will end with `.gz`.

## Collection tools

### Phase stack

As described [here](graal_internals.md), optimization phases are grouped into phase suites and they call each other it a shallow recursive pattern. This tool monitors this and traces which optimization phases were called and when. It outputs a list of phases as they were active.

## Phase dependency matrix

Tracks in which phases were graph nodes created. Then for each phase we get an information from where are the nodes coming so that we can analyse dependency of one phase another one.

## Node type list

Collects a list of all node types seen during this compilation unit. Outputs just a simple text file with a list of Java classes.

## Node type tracker

Tracks types of nodes across optimization phases. For every phase, we want to know, which types of nodes were there when the phase started and exited. And not only types but also their amount.

## Compilation event information

Tracks which method was compiled and how long it took.
