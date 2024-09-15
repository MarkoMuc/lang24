package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Subscript {
    private final HashMap<AstDefn, Term> termMap = new HashMap<>();
    private Term constant;
    private final ArrRef arrRef;
    private boolean linear;
    private int variableCount = 0;

    public Subscript(ArrRef arrRef) {
        this.constant = new Term(0);
        this.arrRef = arrRef;
        linear = true;
    }

    public void addTerm(Term term) {
        if (term.variable == null) {
            if (constant == null) {
                constant = term;
            } else {
                this.constant.coefficient = constant.coefficient + term.coefficient;
            }
        } else {
            if (this.termMap.containsKey(term.variable)) {
                this.termMap.get(term.variable).coefficient += term.coefficient;
            } else {
                variableCount++;
                this.termMap.put(term.variable, term);
            }
        }
    }

    public Collection<Term> getTerms() {
        return termMap.values();
    }

    public Term getConstant() {
        return this.constant;
    }

    public boolean isLinear() {
        return this.linear;
    }

    public void setNonLinear() {
        this.linear = false;
    }

    public int getVariableCount() {
        return this.variableCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(termMap
                .values()
                .stream()
                .map(Term::toString)
                .collect(Collectors.joining("+")));

        sb.append('+').append(constant.toString());

        if (sb.lastIndexOf("+") == sb.length() - 1) {
            sb.replace(sb.length() - 1, sb.length(), "");
        }

        if (sb.indexOf("+") == 0) {
            sb.replace(0, 1, "");
        }
        return sb.toString();
    }
}
