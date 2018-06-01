# FeatureCoPP

FeatureCoPP is a console application written Java&#8482; to decompose variable annotated C-Code into modular structures.
Furthermore, FeatureCoPP merges such modularized structures into an duplicate of the original project.

FeatureCoPP searches a given C project for header (`.h`) and implementation (`.c`) files for C Preprocessor (`CPP`)
conditional directives (`CD`). Each `CD`'s `constExpr` is matched against a given Java&#8482;-regex. If such a match
is successful the controlled code is substituted by a reference link to a newly created role in a physically separated
feature module. This process is conceptually visualized within the following figure
![FCConcept](/doc/resources/img/concept_file_ref_koll.png)
Format: ![Alt Text](url)

## Getting started

### Binary

### Sources

## Usage

## Dependecies

## Regenerating Acceptors

## Limitations
