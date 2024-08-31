parser grammar Lang24Parser;

@header {

	package lang24.phase.synan;
	
	import java.util.*;
	import lang24.common.report.*;
	import lang24.data.token.*;
	import lang24.data.ast.tree.*;
	import lang24.data.ast.tree.defn.*;
	import lang24.data.ast.tree.stmt.*;
	import lang24.data.ast.tree.expr.*;
	import lang24.data.ast.tree.type.*;
	import lang24.phase.lexan.*;

}

@members {

	private Location loc(Token tok) { return new Location((LocLogToken)tok); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((LocLogToken)tok1, (LocLogToken)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((LocLogToken)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1, (LocLogToken)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1, loc2); }

    public void ThrowNewExcp(RecognitionException e, String message){
        throw new Report.Error(new Location(e.getOffendingToken().getLine(),
            e.getOffendingToken().getCharPositionInLine()),
            message + e.getOffendingToken().getText());
    }
	public void ThrowNewError(Token tok, String message) throws Report.Error{
	    throw new Report.Error(loc(tok), message);
	}
}

options{
    tokenVocab=Lang24Lexer;
}

source
returns [AstNode ast]:
    definitions EOF {$ast = $definitions.def;}
    ;

// |——————————————Declarations——————————————|
// Add locations to definitions
definitions
returns [AstNodes def]:
    { Vector<AstNode> defs = new Vector<>(); }
    (
        type_definition
        { defs.add($type_definition.d); }
     | variable_definition
        { defs.add($variable_definition.d); }
     | function_definition
        { defs.add($function_definition.d); }
    )+
    { $def = new AstNodes(defs); }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Definition error around token: ");}

//              |——————————————Type declarations——————————————|
type_definition
returns [AstTypDefn d, Location l]:
    identifier ASSIGN type
    { $l = loc($identifier.l, $type.l); }
    { $d = new AstTypDefn($l, $identifier.id.name, $type.t); }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Type definition error around token: ");}

// 				|——————————————Variable declarations——————————————|
variable_definition
returns [AstVarDefn d, Location l]:
    identifier COLON type
    { $l = loc($identifier.l, $type.l); }
    { $d = new AstVarDefn($l, $identifier.id.name, $type.t); }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Variable definition error around token: ");}

// 				|——————————————Function declarations——————————————|
function_definition
returns [AstFunDefn d, Location l]:
    //identifier LPAR (parameters)? RPAR COLON type (ASSIGN statement (LCBRAC definitions RCBRAC)?)?
    fun_head fun_body
    {
        if($fun_body.l != null) {
            $l = loc($fun_head.l, $fun_body.l);
        } else{
            $l = $fun_head.l;
        }
        $d = new AstFunDefn($l, $fun_head.id.name, $fun_head.pars, $fun_head.t,
                $fun_body.s, $fun_body.def);
    }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Function definition error around token: ");}


// 				|——————————————Statement declarations——————————————|
statement
returns [AstStmt s, Location l]:
    expression_statement { $s = $expression_statement.s; $l = $expression_statement.l; }
    | assign_statement { $s = $assign_statement.s; $l = $assign_statement.l; }
    | if_statement { $s = $if_statement.s; $l = $if_statement.l; }
    | while_statement { $s = $while_statement.s; $l = $while_statement.l; }
    | for_statement { $s = $for_statement.s; $l = $for_statement.l; }
    | vecfor_statement { $s = $vecfor_statement.s; $l = $vecfor_statement.l; }
    | return_statement { $s = $return_statement.s; $l = $return_statement.l; }
    | block_statement { $s = $block_statement.s; $l = $block_statement.l; }
    | decorator_statement { $s = $decorator_statement.s; $l = $decorator_statement.l; }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Statement error around token: ");}

// 				|——————————————Type declarations——————————————|
type
returns [AstType t, Location l]:
    types { $t = $types.atom; $l = $types.l; }
    | array_type { $t = $array_type.arr; $l = $array_type.l; }
    | pointer_type { $t = $pointer_type.ptr; $l = $pointer_type.l; }
    | union_type { $t = $union_type.uni; $l = $union_type.l; }
    | struct_type { $t = $struct_type.str; $l = $struct_type.l; }
    | identifier { $t = $identifier.id; $l = $identifier.l; }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Type syntax error around token: ");}

// 				|——————————————Expression declarations——————————————|

expression
returns [AstExpr e, Location l] :
	conjunctive_expr disjunctive_expr[$conjunctive_expr.e, $conjunctive_expr.l]
	{ $l = $disjunctive_expr.l; $e = $disjunctive_expr.e;}
	; catch [RecognitionException e] {throw new Report.Error(new Location(e.getOffendingToken().getLine(), e.getOffendingToken().getCharPositionInLine()), "Expression Error Around Token : "+ e.getOffendingToken().getText());}
// 			|—————————————————————————————————————————SUBRULES—————————————————————————————————————————|

//|——————————————FUNCTION SUBRULES——————————————|
fun_head
returns [AstNameType id, AstNodes<AstFunDefn.AstParDefn> pars, AstType t, Location l]:
    identifier fun_pars COLON type
    { $id = $identifier.id; $l = loc($identifier.l, $type.l); }
    { $pars = $fun_pars.pars; $t = $type.t ;}
    ;

fun_body
returns [AstStmt s, AstNodes<AstDefn> def, Location l]:
    { $s = null; $l = null; $def = null;}
    (
        ASSIGN statement fun_end
        {
            if($fun_end.l != null){
                $l = loc($ASSIGN, $fun_end.l);
            }else{
                $l = loc($ASSIGN, $statement.l);
            }

            $s = $statement.s;
            if ($fun_end.def != null) {
                $def = $fun_end.def;
            }
        }
    )?
    ;

fun_end
returns [AstNodes<AstDefn> def, Location l]:
        { $def = null; $l = null; }
    (
        LCBRAC definitions RCBRAC
        { $def = $definitions.def; $l = loc($LCBRAC, $RCBRAC); }
    )?
    ;

fun_pars
returns [AstNodes<AstFunDefn.AstParDefn> pars, Location l]:
    { Vector<AstFunDefn.AstParDefn> params = null; }
    { $pars = null;}
    LPAR
    (
        { params = new Vector();}
        parameters
        { params.add($parameters.pars); }
        (
            COMMA p2=parameters
            { params.add($p2.pars); }
        )*
        { $pars = new AstNodes(params); }
    )?
    RPAR
    { $l = loc($LPAR, $RPAR); }
    ;

parameters
returns [AstFunDefn.AstParDefn pars, Location l]:
      ref_par
      { $pars = $ref_par.par; $l = $ref_par.l; }
    | val_par
      { $pars = $val_par.par; $l = $val_par.l; }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Parameter error around token: ");}

ref_par
returns [AstFunDefn.AstRefParDefn par, Location l]:
    POW identifier COLON type
    { $l = loc($POW, $type.l); }
    { $par = new AstFunDefn.AstRefParDefn($l, $identifier.id.name, $type.t); }
    ;

val_par
returns [AstFunDefn.AstValParDefn par, Location l]:
    identifier COLON type
    { $l = loc($identifier.l, $type.l);}
    { $par = new AstFunDefn.AstValParDefn($l, $identifier.id.name, $type.t); }
    ;

// |——————————————STATEMENT SUBRULES——————————————|
expression_statement
returns [AstExprStmt s, Location l] :
	exp=expression SEMI
	{ $l = loc($exp.l, $SEMI); $s = new AstExprStmt($l, $exp.e); }
	;

assign_statement
returns [AstAssignStmt s, Location l] :
	e1=expression ASSIGN e2=expression SEMI
	{ $l = loc($e1.l, $SEMI); }
	{ $s = new AstAssignStmt($l, $e1.e, $e2.e); }
	;

if_statement
returns [AstIfStmt s, Location l] :
	KIF expression KTHEN statement statement_else
	{
	    if ($statement_else.l == null){
	        $l = loc($KIF, $statement_else.l);
	    }else{
	        $l = loc($KIF, $statement.l);
		}
		$s = new AstIfStmt($l, $expression.e, $statement.s, $statement_else.s);
	}
	;

statement_else
returns [AstStmt s, Location l]:
	  { $l = null; $s = null; }
	 (
	    KELSE statement
	    { $s = $statement.s; $l = loc($KELSE, $statement.l); }
	 )?
	;

while_statement
returns [AstWhileStmt s, Location l] :
	KWHILE expression COLON statement
	{
	    $l = loc($KWHILE, $statement.l);
		$s = new AstWhileStmt($l, $expression.e, $statement.s);
	}
	;

for_statement
returns [AstForStmt s, Location l] :
	KFOR LPAR a1=assign_statement expression SEMI a2=assign_statement RPAR COLON statement
	{
	    $l = loc($KFOR, $statement.l);
		$s = new AstForStmt($l, $a1.s, $expression.e, $a2.s, $statement.s);
	}
	;

vecfor_statement
returns [AstVecForStmt s, Location l] :
	KVFOR LPAR ID SEMI lower=expression SEMI upper=expression SEMI step=expression RPAR COLON statement
	{
	    $l = loc($KVFOR, $statement.l);
	    AstNameExpr name = new AstNameExpr(loc($ID), $ID.text);
		$s = new AstVecForStmt($l, name, $lower.e, $upper.e, $step.e, $statement.s);
	}
	;   catch [RecognitionException e] {ThrowNewExcp(e, "For statements syntax is (name, lower_const, upper_const, step_const) ");}

return_statement
returns [AstReturnStmt s, Location l] :
    KRETURN expression SEMI
    { $l = loc($KRETURN, $SEMI); }
    { $s = new AstReturnStmt($l, $expression.e); }
	;

block_statement
returns [AstBlockStmt s, Location l] :
	{ Vector<AstStmt> stmts = new Vector<>(); }
	LCBRAC statement
	{ stmts.add($statement.s); }

	(
		s2=statement
		{ stmts.add($s2.s); }
	)*

	RCBRAC
	{
		$l = loc($LCBRAC, $RCBRAC);
		$s = new AstBlockStmt($l, stmts);
	}
	;

decorator_statement
returns [AstDecoratorStmt s, Location l] :
	{ Vector<AstExpr> exprs = new Vector<>(); }
	DEC_VEC
	LSBRAC
	    expression
	    { exprs.add($expression.e); }
	    (
	        COMMA expression
	        { exprs.add($expression.e); }
	    )*
	RSBRAC
	while_statement
	{
	    $l = loc($DEC_VEC, $while_statement.l);
	    AstNodes<AstExpr> deps = new AstNodes<>(exprs);
        $s = new AstDecoratorStmt($l, deps, $while_statement.s);
	}
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Decorator Statement error around: ");}

//|——————————————TYPE SUBRULES——————————————|
types
returns [AstAtomType atom, Location l] :
	  KVOID
	    { $atom = new AstAtomType(loc($KVOID), AstAtomType.Type.VOID); }
	    { $l = loc($KVOID); }
	| KCHAR
	    { $atom = new AstAtomType(loc($KCHAR), AstAtomType.Type.CHAR); }
	    { $l = loc($KCHAR); }
	| KINT
	    { $atom = new AstAtomType(loc($KINT), AstAtomType.Type.INT); }
	    { $l = loc($KINT); }
	| KBOOL
	    { $atom = new AstAtomType(loc($KBOOL), AstAtomType.Type.BOOL); }
	    { $l = loc($KBOOL); }
	;

union_type
returns [AstUniType uni, Location l] :
	{ Vector<AstRecType.AstCmpDefn> comp = new Vector<>(); }
	LCBRAC component
	{ comp.add($component.cmp); }

	(
		COMMA component
		{ comp.add($component.cmp); }
	)*

	RCBRAC
	{
	    $l = loc($LCBRAC, $RCBRAC);
		AstNodes<AstRecType.AstCmpDefn> comps = new AstNodes<>(comp);
		$uni = new AstUniType($l, comps);
	}
	;

struct_type
returns [AstStrType str, Location l] :
	{ Vector<AstRecType.AstCmpDefn> comp = new Vector(); }
	LPAR component
	{ comp.add($component.cmp); }

	(
		COMMA component
		{ comp.add($component.cmp); }
	)*

	RPAR
	{
	    $l = loc($LPAR, $RPAR);
		AstNodes<AstRecType.AstCmpDefn> comps = new AstNodes<>(comp);
		$str = new AstStrType($l, comps);
	}
	;

component
returns [AstRecType.AstCmpDefn cmp]:
    identifier COLON type
    { $cmp = new AstRecType.AstCmpDefn(loc($identifier.l, $type.l), $identifier.id.name ,$type.t); }
    ; catch [RecognitionException e] {ThrowNewExcp(e, "Component error around token: ");}

array_type
returns [AstArrType arr, Location l] :
    { Vector<AstExpr> dimensions = new Vector(); }
	LSBRAC intconst RSBRAC
	{ dimensions.add($intconst.e); }
	(
	    LSBRAC intconst RSBRAC
	    { dimensions.add($intconst.e); }
	)*
	type
	{
	    $l = loc($LSBRAC, $type.l);
	    $arr = new AstArrType(loc($LSBRAC, $type.l), $type.t, new AstNodes(dimensions));
	}
	;

pointer_type
returns [AstPtrType ptr, Location l] :
	POW type
	{ $ptr = new AstPtrType(loc($POW), $type.t); }
	{ $l = loc($POW, $type.l); }
	;

identifier
returns [AstNameType id, Location l]:
    ID
    { $l = loc($ID); }
    { $id = new AstNameType($l, $ID.text); }
    ;

//|——————————————Expression SUBRULES——————————————|

// |—————DISJUNCTIVE—————|
disjunctive_expr
[AstExpr left, Location ll] returns [AstExpr e, Location l]:
	disjunctive_ops right=conjunctive_expr
	{ $l = loc($ll, $right.l); $left = new AstBinExpr($l, $disjunctive_ops.ops, $left, $right.e); }
	left2=disjunctive_expr[$left, $l]
	{ $e = $left2.e; $l = $left2.l;}
	| { $e = $left; $l = $ll;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Disjunctive expression error around token: ");}

// |—————CONJUNCTIVE—————|
conjunctive_expr
returns [AstExpr e, Location l]:
	relational_expr conjunctive_expr1[$relational_expr.e, $relational_expr.l]
	{ $e = $conjunctive_expr1.e; $l = $conjunctive_expr1.l;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Conjunctive expression error around token: ");}

conjunctive_expr1
[AstExpr left, Location ll] returns [AstExpr e, Location l]:
	conjunctive_ops right=relational_expr
	{ $l = loc($ll, $right.l); $left = new AstBinExpr($l, $conjunctive_ops.ops, $left, $right.e); }
	left2=conjunctive_expr1[$left, $l]
	{ $e = $left2.e; $l = $left2.l;}
	| { $e = $left; $l = $ll;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Conjunctive expression error around token: ");}

// |—————RELATIONAL—————|
relational_expr
returns [AstExpr e, Location l]:
	additive_expr relational_expr1[$additive_expr.e, $additive_expr.l]
	{ $e = $relational_expr1.e; $l = $relational_expr1.l;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Relational expression error around token: ");}

relational_expr1
[AstExpr left, Location ll] returns [AstExpr e, Location l]:
	relational_ops right=additive_expr
	{ $l = loc($ll, $right.l); $left = new AstBinExpr($l, $relational_ops.ops, $left, $right.e); }
	left2=relational_expr1[$left, $l]
	{ $e = $left2.e; $l = $left2.l;}
	| { $e = $left; $l = $ll;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Relational expression error around token: ");}

// |—————ADDITIVE—————|
additive_expr
returns [AstExpr e, Location l]:
	multiplicative_expr additive_expr1[$multiplicative_expr.e, $multiplicative_expr.l]
	{ $e = $additive_expr1.e; $l = $additive_expr1.l;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Additive expression error around token: ");}

additive_expr1
[AstExpr left, Location ll] returns [AstExpr e, Location l]:
	additive_ops right=multiplicative_expr
	{ $l = loc($ll, $right.l); $left = new AstBinExpr($l, $additive_ops.ops, $left, $right.e); }
	left2=additive_expr1[$left, $l]
	{ $e = $left2.e; $l = $left2.l;}
	| { $e = $left; $l = $ll;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Additive expression error around token: ");}
// |—————Multiplicative—————|
multiplicative_expr
returns [AstExpr e, Location l]:
	prefix_expr multiplicative_expr1[$prefix_expr.e, $prefix_expr.l]
	{ $l = $multiplicative_expr1.l; $e = $multiplicative_expr1.e; }
	; catch [RecognitionException e] {ThrowNewExcp(e, "Multiplicative expression error around token: ");}

multiplicative_expr1
[AstExpr left, Location ll] returns [AstExpr e, Location l]:
	mul_ops right=prefix_expr
	{ $l = loc($ll, $right.l); $left = new AstBinExpr($l, $mul_ops.ops, $left, $right.e); }
	left2=multiplicative_expr1[$left, $l]
	{ $e = $left2.e; $l = $left2.l;}
	| { $e = $left; $l = $ll;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Multiplicative expression error around token: ");}

// |—————Prefix—————|
prefix_expr
returns [AstExpr e, Location l]:
	(
	    prefix_ops left2=prefix_expr
	    { $l = loc($prefix_ops.l, $left2.l); $e = new AstPfxExpr($l, $prefix_ops.ops, $left2.e); }
	    |
	    LT type GT left2=prefix_expr
	    { $l = loc($LT, $left2.l); $e = new AstCastExpr($l, $type.t, $left2.e); }
	)
	| postfix_expr { $l = $postfix_expr.l; $e = $postfix_expr.e;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Prefix expression error around token: ");}

// |—————Postfix—————|
postfix_expr
returns [AstExpr e, Location l]:
	other_expr postfix_expr1[$other_expr.e, $other_expr.l]
	{ $e = $postfix_expr1.e; $l = $postfix_expr1.l;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Postfix expression error around token: ");}

postfix_expr1
[AstExpr left, Location ll] returns [AstExpr e, Location l]:
	  LSBRAC expression RSBRAC
	  { $l = loc($ll, $RSBRAC); $left = new AstArrExpr($l, $left, $expression.e); }
	  left2=postfix_expr1[$left, $l]
	  { $e = $left2.e; $l = $left2.l;}
	| POW
	  { $l = loc($ll, $POW); $left = new AstSfxExpr($l, AstSfxExpr.Oper.PTR, $left); }
	  left2=postfix_expr1[$left, $l]
	  { $e = $left2.e; $l = $left2.l;}
	| DOT identifier
	  {$l = loc($ll, $identifier.l); $left = new AstCmpExpr($l, $left, $identifier.id.name); }
	  left2=postfix_expr1[$left, $l]
	  { $e = $left2.e; $l = $left2.l;}
	| { $e = $left; $l = $ll;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Postfix expression error around token: ");}

// |—————OTHER—————|
other_expr
returns [AstExpr e, Location l]:
	  constants { $e = $constants.e; $l = $constants.l;}
	| ID { $l = loc($ID); $e = new AstNameExpr($l, $ID.text);}
	|
		ID
		{ Vector<AstExpr> args = null;}
		LPAR
		{ args = new Vector(); }
		(
            expression { args.add($expression.e);}
            (
                COMMA e2=expression { args.add($e2.e);}
            )*
		)?
		RPAR
		{
		    AstNodes<AstExpr> arguments = null;
		    if(args != null){
		        arguments = new AstNodes<AstExpr>(args);
		    }
		    $l = loc($ID, $RPAR);
			$e = new AstCallExpr($l, $ID.text, arguments);
		}
	| KSIZEOF LPAR type RPAR { $l = loc($KSIZEOF, $RPAR); $e = new AstSizeofExpr($l, $type.t);}
	| LPAR expression RPAR {  $l = loc($LPAR, $RPAR); $e = $expression.e;}
	; catch [RecognitionException e] {ThrowNewExcp(e, "Other expression error around token: ");}
// |—————————————————————————————————————————OPERATORS—————————————————————————————————————————|
constants
returns [AstAtomExpr e, Location l]:
    KNONE { $l = loc($KNONE); $e = new AstAtomExpr($l, AstAtomExpr.Type.VOID, $KNONE.text); }
    | KTRUE { $l = loc($KTRUE); $e = new AstAtomExpr($l, AstAtomExpr.Type.BOOL, $KTRUE.text); }
    | KFALSE { $l = loc($KFALSE); $e = new AstAtomExpr($l, AstAtomExpr.Type.BOOL, $KFALSE.text); }
    | CHARL
        {
        $l = loc($CHARL);
        String value = $CHARL.text;
        String newValue = value;

        int fst = value.indexOf('\'');
        int lst = value.lastIndexOf('\'') == -1 ? value.length() : value.lastIndexOf('\'');
        newValue = value.substring(fst + 1, lst);

        if(newValue.length() == 2) {
            newValue = newValue.substring(newValue.indexOf("\\") + 1);
            if (newValue.indexOf('n') != -1){
                value = "'\\0A'";
            }
        }

        $e = new AstAtomExpr($l, AstAtomExpr.Type.CHAR, value);
        }
    | STRL { $l = loc($STRL); $e = new AstAtomExpr($l, AstAtomExpr.Type.STR, $STRL.text); }
    | KNIL { $l = loc($KNIL); $e = new AstAtomExpr($l, AstAtomExpr.Type.PTR, $KNIL.text); }
    | intconst { $l = $intconst.l; $e = $intconst.e; }
    ;

intconst
returns [AstAtomExpr e, Location l]:
    NUML { $l = loc($NUML); $e = new AstAtomExpr($l, AstAtomExpr.Type.INT, $NUML.text); }
    ;

prefix_ops
returns [AstPfxExpr.Oper ops, Location l]:
	  KNOT { $ops = AstPfxExpr.Oper.NOT; $l = loc($KNOT); }
	| PLUS { $ops = AstPfxExpr.Oper.ADD; $l = loc($PLUS); }
	| MINUS { $ops = AstPfxExpr.Oper.SUB; $l = loc($MINUS); }
	| POW { $ops = AstPfxExpr.Oper.PTR; $l = loc($POW); }
	;

mul_ops
returns [AstBinExpr.Oper ops, Location l] :
	  MUL { $ops = AstBinExpr.Oper.MUL; $l = loc($MUL); }
	| DIV { $ops = AstBinExpr.Oper.DIV; $l = loc($DIV); }
	| MOD { $ops = AstBinExpr.Oper.MOD; $l = loc($MOD); }
	;

additive_ops
returns [AstBinExpr.Oper ops, Location l] :
	  PLUS { $ops = AstBinExpr.Oper.ADD; $l = loc($PLUS); }
	| MINUS { $ops = AstBinExpr.Oper.SUB; $l = loc($MINUS); }
	;

relational_ops
returns [AstBinExpr.Oper ops, Location l] :
	  EQU { $ops = AstBinExpr.Oper.EQU; $l = loc($EQU); }
	| NEQU {$ops = AstBinExpr.Oper.NEQ; $l = loc($NEQU); }
	| LT { $ops = AstBinExpr.Oper.LTH; $l = loc($LT); }
	| GT { $ops = AstBinExpr.Oper.GTH; $l = loc($GT); }
	| LTE { $ops = AstBinExpr.Oper.LEQ; $l = loc($LTE); }
	| GTE { $ops = AstBinExpr.Oper.GEQ; $l = loc($GTE); }
	;

conjunctive_ops
returns [AstBinExpr.Oper ops, Location l]:
	KAND { $ops = AstBinExpr.Oper.AND; $l = loc($KAND); }
	;

disjunctive_ops
returns [AstBinExpr.Oper ops, Location l]:
	KOR { $ops = AstBinExpr.Oper.OR; $l = loc($KOR); }
	;
