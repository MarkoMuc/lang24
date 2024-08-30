package lang24.data.ast.tree.expr;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.visitor.*;

/**
 * A function call.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class AstCallExpr extends AstExpr {

	/** The name. */
	public String name;

	/** The arguments. */
	public final AstNodes<AstExpr> args;

	/**
	 * Constructs a function call.
	 * 
	 * @param location The location.
	 * @param name     The name.
	 * @param args     The arguments.
	 */
	public AstCallExpr(Locatable location, String name, AstNodes<AstExpr> args) {
		super(location);
		this.name = name;
		this.args = args;
	}

	@Override
	public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
		return visitor.visit(this, arg);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(AstExpr arg : args) {
			if (i > 0){
				sb.append(", ");
			}
			sb.append(arg.toString());
			i++;
		}
		return name + "(" + sb + ")";
	}
}