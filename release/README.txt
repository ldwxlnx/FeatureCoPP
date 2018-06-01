## USAGE
- manual invocation: java -jar FeatureCoPP.jar <mode> <inputdir> [<regex>]
  <mode> ::= --report | --areport | --split | --asplit | --merge-scripts
  <inputdir> ::= relative or absolute path to c project (symlinks are ignored!)
		- default: current working directory
  <regex> ::= java regular expression (e.g., ".*" matches all constExpr, ".*CONFIG_.*" matches all constExpr containing CONFIG_ somewhere)
		NOTA BENE:
		- default: ".*"
		- Quote special regex characters if needed literally!
		- Enclose pathnames and patterns containing whitespace (who does that?;-) in double quotes ""!
  
## HELPERS
- split-scripts perform controlled-code-separation
- asplit-scripts perform additional (time consuming) statistical syntax analysis of controlled code
- report and areport scripts perform generate xml journal only (with _a_nalysis and without)
- merge-scripts reintegrate modules into one code base (expecting <dir>_split as input which should reside next to the original project)
- search pattern is java regular expression, hence remember to quote special character e.g. defined\(CONFIG_PM\)
- add files to be ignored by FeatureCoPP to conf.d/blacklist.conf with absoulte path names