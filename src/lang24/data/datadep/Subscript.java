package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;

import java.util.HashMap;
import java.util.Vector;
import java.util.stream.Collectors;

public class Subscript {
    public Vector<Term> terms = new Vector<>();
    public HashMap<AstDefn, Term> termMap = new HashMap<>();
    public Term constant;

    public Subscript() {
        this.constant = new Term(0);
    }

    public void addTermHash(Term term) {
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
                this.termMap.put(term.variable, term);
            }
        }
    }

    public void addTerm(Term term) {
        this.terms.add(term);
        addTermHash(term);
    }

    public void collect() {
        Vector<Term> tmp = terms.stream()
                .filter(x -> x.variable == null)
                .collect(Collectors.toCollection(Vector::new));
        if (tmp.size() > 1) {
            Term last = new Term(
                    tmp.stream()
                            .map(t -> t.coefficient)
                            .reduce(0, Integer::sum));
            terms.removeAll(tmp);
            terms.add(last);
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(terms.stream()
                .filter(x -> x.variable != null)
                .map(Term::toString)
                .collect(Collectors.joining("+")));

        sb.append("+").append(terms.stream()
                .filter(x -> x.variable == null)
                .map(Term::toString)
                .collect(Collectors.joining("+")));

        if (sb.lastIndexOf("+") == sb.length() - 1) {
            sb.replace(sb.length() - 1, sb.length(), "");
        }

        if (sb.indexOf("+") == 0) {
            sb.replace(0, 1, "");
        }
        return sb.toString();
    }

    public String toString2() {
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
