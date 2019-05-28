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
project. FeatureCoPP assumes the original project location next to `<inputdir>_split` and deduces its name automatically.
For instance, given `/tmp/foo_split` as input, FeatureCoPP assumes the original project as `/tmp/foo`.
Now all changes within the feature modules are file-wise superimposed (i.e., overwritten) onto their sibling files within the
original project. The output is written to `<inputdir>_merge`. If no changes have been made within the feature modules, `<inputdir>` and `<input>_merge` are structurally and textually identical.

### Calculating Physical Separation Potential (a.k.a. the `a`-modes)

FeatureCoPP can also perform a syntactical analysis of controlled code within requested `CD`s and ranks the found syntactical
structures regarding bottom-up program comprehension. The calculated values are a heuristic to assist
developers in their decision, which features are suitable for physical separation. This heuristic - namely `PSPOT` (Physical Separation Potential) - is an additive compound value consisting of two sub-heuristics:
1. `ER` (Encapsulation Ratio)
	Represents the ratio of declared and used symbols to all used symbols within a particular `CD`. Hence, `ER` is real 
	within the interval [0,1].
2. `CS` (Comprehension Support).
	Ranks found syntactic structures against a recommendation vector by calculating the respective cosine similarity.
	The recommendation vector looks as follows:
	
	| structure | rank | interpretation |
	| --- | --- | --- |
	| funcdef | 6 | modular unit, good encapsulation, probably easy to maintain in isolation |
	| struct-/uniondecl | 5 | compound type, possibly high declarative impact | 
	| funcdecl | 4 | forward declaration explaining signatures, probably useful maintaining controlled code fragments over function level | 
	| vardecl | 3 | explaining type and scope of symbols in physically separated roles |
	| stmts | 2 | functional volume of controlled code fragment | 
	| comment | 1 | additional helpers, useful as beacons during maintenance | 
	
	Simply put, the more of the above syntactical structures a role provides the higher it is ranked.
	Since only natural values can occur the expected values lie in between [0, 1].
		
Based on these heuristics Physical Separation Potential is calculated as `PSPOT = ER + CS`, which results in values in between [0,2].
Roles ranked with 0 are considered having no potential for physical separation. Roles within (0, 1] are considered having a
limited physical separation potential and roles within [1, 2] are considered having a high potential for physical separation.

The above mentioned analyses and calculation have a high impact on FeatureCoPP's runtime and can be invoked by `--asplit` and
`--areport` ([Refer to Usage](#usage)).

## Getting started

FeatureCoPP is brought to you as binary or as Eclipse&#8482; source project. For a "hit and run" experience we suggest to prefer the
binaries over sources. 

### Binary

To work with the binaries proceed as follows:

1. Download the current stable build [here](https://github.com/ldwxlnx/FeatureCoPP/releases). 
2. Extract the respective archive to your preferred filesystem location.
3. Open your favorite terminal emulator and `cd` into the extracted directory.
4. Issue a chmod 700 on the invocation shell scripts (`*.sh`) on *nix environments having a `bash`.
5. Refer to [Usage](#usage) for explanations regarding invocation of FeatureCoPP.

### Sources

FeatureCoPP is brought to you as Eclipse&#8482; project. After a successful `git clone` an automatic project import is suggested by
the IDE. Please refer to your IDE's documentation, if you use something different than Eclipse&#8482;.
Project's structure is pretty much standard:
1. `src` - clearly the sources (package-wise)
2. `doc` - doxygen configuration (prepared for generation, requires doxygen and graphviz install)
3. `lib` - additional library dependencies
4. `release` - generate your own build here using Apache Ant&#8482; with provided `build.xml`
5. `specs` - JFlex and JCup lexer and parser specification files
6. `test_dos` - Test input having `\r\n` line terminators
7. `test_unix` - Test input having `\n` line terminators
8. `conf.d` - configuration directory (multiple example configuration files for batch processing FeatureCoPP)
9. `blacklist_examples` - example files which are refused by FeatureCoPP

We strongly recommend to setup different run configurations for each mode and test project (cf. [Usage](#usage)).

## Usage

You can invoke FeatureCoPP on your terminal emulator directly as explained within the following subsections or by the provided wrapper scripts. Using FeatureCoPP from a given IDE requires you to setup the program arguments corresponding to the following explanations.

### Physical Separation
`java -jar FeatureCoPP.jar --split | --asplit <inputdir> [<regex>]`

`--split` - Performs physical separation without `PSPOT` calculation.

`--asplit` - Performs physical separation with `PSPOT` calculation.

`<inputdir>` - C source project

`<regex>` - Java&#8482; compliant regular expression matching the `constExpr` of a conditional directive (default: ".*")

If whitespaces occur in path names or regular expression, you should enclose them in double quotes "".

### Re-Integration
`java -jar FeatureCoPP.jar --merge <inputdir>_split` 

Merges given directory in conjunction with original project (`<inputdir>` residing next to `<inputdir>_split`) into a new
project `<inputdir>_merged`. 

### Preview
`java -jar FeatureCoPP.jar --report | --areport <inputdir> [<regex>]`

Analogously to [Physical Separation](#physical-separation) but without actually performing extraction. This mode generates only
the XML journal (cf. [Reporting](#reporting)) with and without syntactical analysis.

## Dependencies

FeatureCoPP relies on the following libraries:

| lib | version | purpose | resource | shipped |
| --- | --- | --- | --- | --- |
| Apache Ant&#8482; | 1.10.1 | automated build of binaries | [Apache Ant](https://ant.apache.org/) | no |
| JFlex | 1.6.1 | ant task to generate lexer | [JFlex](http://jflex.de/) | yes |
| JCup | 11a | ant task to generate parser | [JCup](http://www2.cs.tum.edu/projects/cup/) | yes |
| Eclipse CDT | 5.6.0 | c-parser framework used for statistical analysis | [Eclipse CDT](https://www.eclipse.org/cdt/) | yes |
| Choco Solver | 4.0.4 | CSP solver to find macro settings to apply CDT parser | [Choco](https://github.com/chocoteam/choco-solver) | yes |

For modifications of FeatureCoPP regarding its lexers and parsers, you should install Apache Ant&#8482; first and perform a setup corresponding to the respective documentation.
All libraries are either built into the [Release](https://github.com/ldwxlnx/FeatureCoPP/releases) of FeatureCoPP or shipped alongside sources in folder `lib`. The shipped Eclipse project has already setup respective jar dependencies. In case the jar inclusions do not work for you, add the following jars by `project properties->java build path-libraries->add jars...` by selecting them from the `lib`folder:

![LibDep](/doc/resources/img/libdep.png) 

## Reporting

FeatureCoPP has the following reporting facilities.

### Logs

During each run a timestamped log file `FeatureCoPP_<mode>.log` is produced, which shows the significant program activities and possible error situations. Since no log rotation is currently implemented for FeatureCoPP, every invocation overwrites the corresponding log file for the respective mode. Additionally the informational (`[INFO]`) log output is also written `stdout` and errors (`[FAIL]`) are written to `stderr`.

The modes `asplit` and `areport` additional produce two further logs. The log `FeatureCoPP_ast.log` shows the respective detected C ASTs for each role. The log `FeatureCoPP_csp.log` contains information about the behavior of Choco's CSP solver.
Both files can grow to a remarkable size, even on small sized source projects. Therefore, make sure you have a reasonable amount of disk space on the partition where you invoke FeatureCoPP. 

### Report

The modes `asplit`, `split`, `areport` and `report` write a XML journal to the output directory `<inputdir>_split named `FeatureCoPP_report.xml` as shown below.
	
![XMLJ](/doc/resources/img/xmljournal.png)

This journal shows the heuristic values for each role and average and standard deviation for each feature module. Furthermore, overall statistics are presented regarding detected `CD`s, number of (requested) features and many more. An additional tool for human readable reporting on these XML journals is soon-to-be provided by our tool [FCJournalReader](https://github.com/ldwxlnx/FCJournalReader).

## Regenerating Acceptors

The project directory `specs`contains lexer and parser specifications. In case you want to modify the behavior of lexical and syntactical analysis, you have to regenerate the respective Java classes. This is done by the respective Ant `build.xml`s residing within the corresponding subdirectories. After a successful generation of the respective acceptor the corresponding Java class is written into the source tree. Issue a `Refresh` on your project tree, to make Eclipse aware of the newly generated classes.

## Related Projects

FeatureCoPP is successfully used in [PCLocator](https://github.com/ekuiter/PCLocator).

## Related Publications

The concept of FeatureCoPP was firstly introduced in [Proceedings of the 7th International Workshop on Feature-Oriented Software Development](https://dl.acm.org/citation.cfm?id=3001876). Further publications are currently under review and will follow soon.
