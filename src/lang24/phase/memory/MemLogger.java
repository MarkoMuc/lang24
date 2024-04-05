package lang24.phase.memory;

import lang24.common.logger.*;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.visitor.*;

/**
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemLogger implements AstNullVisitor<Object, Object> {

	/** The logger the log should be written to. */
	private final Logger logger;

	/**
	 * Construct a new visitor with a logger the log should be written to.
	 * 
	 * @param logger The logger the log should be written to.
	 */
	public MemLogger(final Logger logger) {
		this.logger = logger;
	}

	// lang24.data.ast.tree.defn:

	@Override
	public Object visit(AstVarDefn varDefn, Object arg) {
		if (Memory.varAccesses.get(varDefn) == null)
			return null;
		Memory.varAccesses.get(varDefn).log(logger);
		return null;
	}

	@Override
	public Object visit(AstFunDefn funDefn, Object arg) {
		if (Memory.frames.get(funDefn) == null)
			return null;

		Memory.frames.get(funDefn).log(logger);
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
		if (Memory.parAccesses.get(refParDefn) == null)
			return null;
		Memory.parAccesses.get(refParDefn).log(logger);
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
		if (Memory.parAccesses.get(valParDefn) == null)
			return null;
		Memory.parAccesses.get(valParDefn).log(logger);
		return null;
	}

	@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
		if (Memory.cmpAccesses.get(cmpDefn) == null)
			return null;
		Memory.cmpAccesses.get(cmpDefn).log(logger);
		return null;
	}

}
