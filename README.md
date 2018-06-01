# FeatureCoPP

FeatureCoPP is a console application written Java&#8482; to decompose variable annotated C-Code into modular structures.
Furthermore, FeatureCoPP merges such modularized structures into an duplicate of the original project.

## Concept

FeatureCoPP searches a given C project for header (`.h`) and implementation (`.c`) files for C Preprocessor (`CPP`)
conditional directives (`CD`). Each `CD`'s `constExpr` is matched against a given Java&#8482;-regex. If such a match
is successful the controlled code is substituted by a reference link to a newly created role containing the respective
controlled code in a physically separated feature module. This process is conceptually visualized within the following figure:

![FCConcept](/doc/resources/img/concept_file_ref_koll.png)

### Physical Separation (Extraction a.k.a `--split`)

File `src.c` contains four `CD`s (three of them nested). The controlled code is symbolized by three dots (`...`).
FeatureCoPP now extracts each of these controlled code fragments into distinct feature modules within a new folder structure
`<inputdir>_split`. The nesting structure is preserved so that a feature module (e.g., `Feature A`) refers to other roles
in different feature modules (e.g., `Feature B`) or to another role within the same feature module (cf. the doubly nested
`#ifdef A`).

### Re-assembly (Re-Integration a.k.a `--merge`)

## Getting started

### Binary

### Sources

## Usage

## Dependecies

## Regenerating Acceptors

## Limitations
