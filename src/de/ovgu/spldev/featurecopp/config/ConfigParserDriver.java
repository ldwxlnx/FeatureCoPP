package de.ovgu.spldev.featurecopp.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;

import de.ovgu.spldev.featurecopp.config.ConfigParser.ConfigParserException;
import de.ovgu.spldev.featurecopp.config.Configuration.UserConf;

/**
 * Parser driver for conditional inspection
 * @author K. Ludwig
 *
 */
public final class ConfigParserDriver {
	public ConfigParserDriver(String configFilename, UserConf defaultConfig) throws FileNotFoundException {
		lexer = new ConfigLexer(new FileReader(configFilename));
		parser = new ConfigParser(lexer);
		parser.setDefaultConf(defaultConfig);
	}
	public HashSet<UserConf> run(boolean showLexerOutput) throws ConfigParserException {
		lexer.debug(showLexerOutput);
		try {
			parser.parse();
		} catch (Exception e) {
			throw new ConfigParserException(e.getMessage());
		}
		HashSet<UserConf> config = parser.getUserConf();
		if(config == null) {
			// should never happen!
			throw new ConfigParser.ConfigParserException("No configuration generated");
		}
		config.forEach((userconf) -> {
			try {
				userconf.fixateBindings();
			} catch (IllegalArgumentException | SecurityException
					| IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		});		
		
		return config;
	}
	/** generated parser */
	private ConfigParser parser;
	private ConfigLexer lexer;
}
