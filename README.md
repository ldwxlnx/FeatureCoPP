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

FeatureCoPP can revert the physical separation based on a given already split (`<inputdir>_split`) directory and the original
project. FeatureCoPP assumes original project location next to `<inputdir>_split` and deduces its name automatically.
For instance, given `/tmp/foo_split` as input, FeatureCoPP assumes the original project as `/tmp/foo`.
Now all changes within the feature modules are file-wise superimposed (i.e., overwritten) onto their sibling files within the
original project. The output is written to `<inputdir>_merge`. If no changes have been made within the feature modules, `<inputdir>` and `<input>_merge` are structurally and textually identical ([Refer to limitations](#limitations)).

### Calculating Physical Separation Potential (a.k.a. the `a`-modes)

FeatureCoPP can also perform a syntactical analysis of controlled code within requested `CD`s and ranks the found syntactical
structures regarding bottom-up program comprehension. The calculated values are a heuristic to assist
developers in their decision, which features are suitable for physical separation. This heuristic - namely `PSPOT` (Physical Separation Potential) - is a additive compound value consisting of two sub-heuristics:
1. `ER` (Encapsulation Ratio)
	Represents the ratio of declared and used symbols to all used symbols within a particular `CD`. Hence, `ER` is rational 
	within the interval [0,1].
2. `CS` (Comprehension Support).
	Ranks found syntactic structures against a recommendation vector by calculating the respective cosine similarity.
	The recommendation vector looks as follows:
	structure | rank | interpretation
	----------|------|---------------
	funcdef | 6 | modular unit, good encapsulation, probably easy to maintain in isolation
	struct-/uniondecl | 5 | compound type, possibly high declarative impact
	funcdecl | 4 | forward declaration explaining signatures, probably useful maintaining controlled code fragments over function level
	vardecl | 3 | explaining type and scope of symbols in physically separated roles
	stmts | 2 | functional volume of controlled code fragment
	comment | 1 | additional helpers, useful as beacons during maintenance
3. `PSPOT = ER + CS`

## Getting started

### Binary

### Sources

## Usage

## Dependecies

## Regenerating Acceptors

## Limitations
