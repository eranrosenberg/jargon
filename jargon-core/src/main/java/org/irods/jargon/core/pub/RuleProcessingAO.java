package org.irods.jargon.core.pub;

import java.util.List;

import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.domain.DelayedRuleExecution;
import org.irods.jargon.core.rule.IRODSRuleExecResult;
import org.irods.jargon.core.rule.IRODSRuleParameter;
import org.irods.jargon.core.rule.JargonRuleException;
import org.irods.jargon.core.rule.RuleInvocationConfiguration;

public interface RuleProcessingAO extends IRODSAccessObject {

	/**
	 * Type of rule invocation, primarily involving special handling of iRODS old
	 * style rules. For other rule languages DEFAULT is the usual setting.
	 *
	 * @author conwaymc
	 *
	 */
	public enum RuleProcessingType {
		INTERNAL, EXTERNAL, CLASSIC, DEFAULT
	}

	/**
	 * List the available rule engines (python, classic, etc)
	 * 
	 * @return {@code List} of {@code String} with each configured rule engine
	 * @throws JargonException
	 *             {@link JargonException}
	 */
	List<String> listAvailableRuleEngines() throws JargonException;

	/**
	 * Submit a user-defined rule for processing (analogous to running irule). Note
	 * that this method will, if running on iRODS 3.0 or higher, add the @external
	 * flag, which is explained below. The @external flag is used when processing
	 * rules in the new rule format, and is not in effect (or causes any harm) if
	 * executing a 'classic' style rule.
	 * <p>
	 * The difference between having @external not having it is how the rule engine
	 * interprets string input parameters. For external, the input parameters are
	 * "code", for internal the input parameters are "values". For example, if you
	 * have,
	 * <p>
	 * INPUT *A=10
	 * <p>
	 * The packed binary/XML will have a string 1.
	 * <p>
	 * If the rule is external, 10 is interpreted as "code", so the value is integer
	 * 10.
	 * <p>
	 * If the rule is internal, 10 is interpreted as a "value", so the value is
	 * string "10".
	 *
	 *
	 * @param irodsRuleAsString
	 *            {@code String} containing an iRODS rule as would be submitted via
	 *            irule
	 * @return {@link IRODSRuleExecResult} with the rule response
	 * @throws JargonRuleException
	 *             error in translation of the rule, typically syntax
	 * @throws JargonException
	 *             other error in irods or jargon
	 */
	IRODSRuleExecResult executeRule(final String irodsRuleAsString) throws JargonRuleException, JargonException;

	/**
	 * <p>
	 * <b>Note: this method purges ALL rules in the queue</b>
	 *
	 * @return {@code int} with a count of the rules purged from the delayed
	 *         execution queue
	 * @throws JargonException
	 *             for iRODS error
	 */
	int purgeAllDelayedExecQueue() throws JargonException;

	/**
	 * List all the rules in the delay exec queue
	 *
	 * @param partialStartIndex
	 *            {@code int} with 0 or an offset
	 * @return List of {@code DelayedRuleExecution}
	 * @throws JargonException
	 *             for iRODS error
	 */
	List<DelayedRuleExecution> listAllDelayedRuleExecutions(int partialStartIndex) throws JargonException;

	/**
	 * Given a rule file that exists as a resource at a given path visible to the
	 * class loader. This will be a rule file that will be executed. The input
	 * parameters passed in can be used to override parameters discovered in the
	 * rule body.
	 * <p>
	 * The rule will be run based on the default values in the given rule,
	 * overridden by any values passed in as an input parameter.
	 *
	 * @param resourcePath
	 *            {@code String} that will be a path from which the resource will be
	 *            loaded, using the rules associated with
	 *            {@code Class.getResourceAsStream(String)}.
	 * @param irodsRuleInputParameters
	 *            {@code List} of {@link IRODSRuleParameter} with overrides to
	 *            parameters defined in the rule file. This can be set to
	 *            {@code null} if no overrides are needed.
	 * @param ruleProcessingType
	 *            {@link RuleProcessingAO.RuleProcessingType} that describes how
	 *            parameters are resolved (@internal, @external, classic for rules
	 *            in the classic rule language, pre iRODS 3.0).
	 * @return {@link IRODSRuleExecResult}
	 * @throws DataNotFoundException
	 *             if rule missing
	 * @throws JargonException
	 *             for iRODS error
	 */
	IRODSRuleExecResult executeRuleFromResource(String resourcePath, List<IRODSRuleParameter> irodsRuleInputParameters,
			RuleProcessingType ruleProcessingType) throws DataNotFoundException, JargonException;

	/**
	 * Given a rule file that exists as a resource at a given path visible to the
	 * class loader. This will be a rule file that will be executed. The input
	 * parameters passed in can be used to override parameters discovered in the
	 * rule body.
	 * <p>
	 * The rule will be run based on the default values in the given rule,
	 * overridden by any values passed in as an input parameter.
	 *
	 * @param resourcePath
	 *            {@code String} that will be a path from which the resource will be
	 *            loaded, using the rules associated with
	 *            {@code Class.getResourceAsStream(String)}.
	 * @param irodsRuleInputParameters
	 *            {@code List} of {@link IRODSRuleParameter} with overrides to
	 *            parameters defined in the rule file. This can be set to
	 *            {@code null} if no overrides are needed.
	 * @param ruleInvocationConfiguration
	 *            {@link RuleInvocationConfiguration} that describes the rule type
	 *            and processing parameters, including the rule engine instance it
	 *            relates to
	 * @return {@link IRODSRuleExecResult}
	 * @throws DataNotFoundException
	 *             if rule missing
	 * @throws JargonException
	 *             for iRODS error
	 */
	IRODSRuleExecResult executeRuleFromResource(String resourcePath, List<IRODSRuleParameter> irodsRuleInputParameters,
			RuleInvocationConfiguration ruleInvocationConfiguration) throws DataNotFoundException, JargonException;

	/**
	 * Given a rule file that exists as an iRODS file. This will be a rule file that
	 * will be executed. The input parameters passed in can be used to override
	 * parameters discovered in the rule body.
	 * <p>
	 * The rule will be run based on the default values in the given rule,
	 * overridden by any values passed in as an input parameter.
	 *
	 * @param ruleFileAbsolutePath
	 *            {@code String} with the absolute path to a file in iRODS
	 *            containing the rule
	 * @param irodsRuleInputParameters
	 *            {@code List} of {@link IRODSRuleParameter} with overrides to
	 *            parameters defined in the rule file. This can be set to
	 *            {@code null} if no overrides are needed.
	 * @param ruleInvocationConfiguration
	 *            {@link RuleInvocationConfiguration} that describes the rule type
	 *            and processing parameters, including the rule engine instance it
	 *            relates to
	 * @return {@link IRODSRuleExecResult}
	 * @throws JargonException
	 *             for iRODS error
	 */
	IRODSRuleExecResult executeRuleFromIRODSFile(String ruleFileAbsolutePath,
			List<IRODSRuleParameter> irodsRuleInputParameters, RuleInvocationConfiguration ruleInvocationConfiguration)
			throws JargonException;

	/**
	 * Given a rule file that exists as an iRODS file. This will be a rule file that
	 * will be executed. The input parameters passed in can be used to override
	 * parameters discovered in the rule body.
	 * <p>
	 * The rule will be run based on the default values in the given rule,
	 * overridden by any values passed in as an input parameter.
	 *
	 * @param ruleFileAbsolutePath
	 *            {@code String} with the absolute path to a file in iRODS
	 *            containing the rule
	 * @param irodsRuleInputParameters
	 *            {@code List} of {@link IRODSRuleParameter} with overrides to
	 *            parameters defined in the rule file. This can be set to
	 *            {@code null} if no overrides are needed.
	 * @param ruleProcessingType
	 *            {@link RuleProcessingAO.RuleProcessingType} that describes how
	 *            parameters are resolved (@internal, @external, classic for rules
	 *            in the classic rule language, pre iRODS 3.0).
	 * @return {@link IRODSRuleExecResult}
	 * @throws JargonException
	 *             for iRODS error
	 */
	IRODSRuleExecResult executeRuleFromIRODSFile(String ruleFileAbsolutePath,
			List<IRODSRuleParameter> irodsRuleInputParameters, RuleProcessingType ruleProcessingType)
			throws JargonException;

	/**
	 * Execute an iRODS rule, specifying the parameter processing type, and allowing
	 * the provision of overrides to rule input parameters that will override
	 * parameters derived by processing the rule body. This method applies to either
	 * the classic (pre iRODS 3.0) and new (as of iRODS 3.0) rule language.
	 * <p>
	 * Note that the rule processing type needs to be set according to the type of
	 * rule being processed. The 'classic' rule processing type is meant for the
	 * 'classic' rule language. The {@code EXTERNAL} and {@code INTERNAL}.
	 * <p>
	 * * Note that this method will, if running on iRODS 3.0 or higher, add
	 * the @external flag, which is explained below. The @external flag is used when
	 * processing rules in the new rule format, and is not in effect effect (or
	 * causes any harm) if executing a 'classic' style rule.
	 * <p>
	 * The difference between having @external not having it is how the rule engine
	 * interprets string input parameters. For external, the input parameters are
	 * "code", for internal the input parameters are "values". For example, if you
	 * have,
	 * <p>
	 * INPUT *A=10
	 * <p>
	 * The packed binary/XML will have a string 1.
	 * <p>
	 * If the rule is external, 10 is interpreted as "code", so the value is integer
	 * 10.
	 * <p>
	 * If the rule is internal, 10 is interpreted as a "value", so the value is
	 * string "10".
	 *
	 * @param irodsRuleAsString
	 *            {@code String} with the rule body, as well as input and output
	 *            parameters.
	 * @param inputParameterOverrides
	 *            {@code List} of {@link IRODSRuleParameter} that overrides the
	 *            parameters derived from the rule bodies.
	 * @param ruleProcessingType
	 *            {@link RuleProcessingAO.RuleProcessingType} enum value. Note that
	 *            it should be set to {@code CLASSIC} for classic rules, and
	 *            {@code EXTERNAL} or {@code INTERNAL} for new format rules.
	 * @return {@link IRODSRuleExecResult} that represents the results of processing
	 *         the rule.
	 * @throws JargonRuleException
	 *             if an exception occurred in rule translation.
	 * @throws JargonException
	 *             if iRODS processing resulted in an error.
	 * @deprecated use the method signature that adds
	 *             {@link RuleInvocationConfiguration} instead. This change can
	 *             accommodate other rule engine plugins and associated additional
	 *             data for each
	 */
	@Deprecated
	IRODSRuleExecResult executeRule(String irodsRuleAsString, List<IRODSRuleParameter> inputParameterOverrides,
			RuleProcessingType ruleProcessingType) throws JargonRuleException, JargonException;

	/**
	 * See above comment for details. This variant adds an
	 * <code>IrodsRuleInvocationTypeEnum</code> value that can cue Jargon on which
	 * rule engine (python, irods) the rule is bound for. In the above method, the
	 * invocation type defaults to auto and Jargon will try and do some simple
	 * checks to guess what type of rule is being processed.
	 *
	 * @param irodsRuleAsString
	 *            {@code String} with the rule body, as well as input and output
	 *            parameters.
	 * @param inputParameterOverrides
	 *            {@code List} of {@link IRODSRuleParameter} that overrides the
	 *            parameters derived from the rule bodies.
	 *
	 * @param ruleInvocationConfiguration
	 *            {@link RuleInvocationConfiguration} that represents the type of
	 *            rule engine the user wishes this rule to be run on. With multiple
	 *            rule engines iRODS needs this information to properly process the
	 *            rule. If set to AUTO_DETECT Jargon will try and guess (but may not
	 *            be too smart at first).
	 * @return {@link IRODSRuleExecResult} that represents the results of processing
	 *         the rule.
	 * @throws JargonRuleException
	 *             if an exception occurred in rule translation.
	 * @throws JargonException
	 *             if iRODS processing resulted in an error.
	 */
	IRODSRuleExecResult executeRule(String irodsRuleAsString, List<IRODSRuleParameter> inputParameterOverrides,
			final RuleInvocationConfiguration ruleInvocationConfiguration) throws JargonRuleException, JargonException;

	/**
	 * Delete selected rule from the delayed execution queue.
	 * <p>
	 * This method will silently ignore a rule not on the queue
	 *
	 * @param queueId
	 *            {@code int} with an id that relates to a stored rule to be removed
	 * @throws JargonException
	 *             for iRODS error
	 */
	void purgeRuleFromDelayedExecQueue(int queueId) throws JargonException;

}
