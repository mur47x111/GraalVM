/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// The content of this file is automatically generated. DO NOT EDIT.

package com.oracle.truffle.dsl.processor.expression;

import java.util.*;
import java.io.*;
import java.nio.charset.*;

import com.oracle.truffle.dsl.processor.expression.DSLExpression.*;

// Checkstyle: stop
// @formatter:off
class Parser {
	public static final int _EOF = 0;
	public static final int _identifier = 1;
	public static final int _numericLiteral = 2;
	public static final int maxT = 15;

    static final boolean _T = true;
    static final boolean _x = false;
    static final int minErrDist = 2;

    public Token t; // last recognized token
    public Token la; // lookahead token
    int errDist = minErrDist;

    public final Scanner scanner;
    public final Errors errors;
    
    
    public Parser(InputStream input) {
        this.scanner = new Scanner(input);
        errors = new Errors();
    }

    void SynErr(int n) {
        if (errDist >= minErrDist)
            errors.SynErr(la.line, la.col, n);
        errDist = 0;
    }

    public void SemErr(String msg) {
        if (errDist >= minErrDist)
            errors.SemErr(t.line, t.col, msg);
        errDist = 0;
    }

    void Get() {
        for (;;) {
            t = la;
            la = scanner.Scan();
            if (la.kind <= maxT) {
                ++errDist;
                break;
            }

            la = t;
        }
    }

    void Expect(int n) {
        if (la.kind == n)
            Get();
        else {
            SynErr(n);
        }
    }

    boolean StartOf(int s) {
        return set[s][la.kind];
    }

    void ExpectWeak(int n, int follow) {
        if (la.kind == n)
            Get();
        else {
            SynErr(n);
            while (!StartOf(follow))
                Get();
        }
    }

    boolean WeakSeparator(int n, int syFol, int repFol) {
        int kind = la.kind;
        if (kind == n) {
            Get();
            return true;
        } else if (StartOf(repFol))
            return false;
        else {
            SynErr(n);
            while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
                Get();
                kind = la.kind;
            }
            return StartOf(syFol);
        }
    }

	DSLExpression  Expression() {
		DSLExpression  result;
		result = LogicFactor();
		return result;
	}

	DSLExpression   LogicFactor() {
		DSLExpression   result;
		result = ComparisonFactor();
		if (la.kind == 3) {
			Get();
			Token op = t; 
			DSLExpression  right = ComparisonFactor();
			result = new Binary(op.val, result, right); 
		}
		return result;
	}

	DSLExpression   ComparisonFactor() {
		DSLExpression   result;
		result = NegateFactor();
		if (StartOf(1)) {
			switch (la.kind) {
			case 4: {
				Get();
				break;
			}
			case 5: {
				Get();
				break;
			}
			case 6: {
				Get();
				break;
			}
			case 7: {
				Get();
				break;
			}
			case 8: {
				Get();
				break;
			}
			case 9: {
				Get();
				break;
			}
			}
			Token op = t; 
			DSLExpression  right = NegateFactor();
			result = new Binary(op.val, result, right); 
		}
		return result;
	}

	DSLExpression   NegateFactor() {
		DSLExpression   result;
		boolean negated = false; 
		if (la.kind == 10) {
			Get();
			negated = true; 
		}
		result = Factor();
		result = negated ? new Negate(result) : result;
		return result;
	}

	DSLExpression  Factor() {
		DSLExpression  result;
		result = null; 
		if (la.kind == 1) {
			result = MemberExpression(result);
		} else if (la.kind == 2) {
			Get();
			result = new IntLiteral(t.val); 
		} else if (la.kind == 11) {
			Get();
			result = Expression();
			Expect(12);
		} else SynErr(16);
		return result;
	}

	DSLExpression  MemberExpression(DSLExpression receiver) {
		DSLExpression  result;
		result = null; 
		Expect(1);
		Variable variable = new Variable(receiver, t.val); 
		result = variable; 
		if (la.kind == 11) {
			Get();
			List<DSLExpression> parameters = new ArrayList<>();
			DSLExpression parameter; 
			if (StartOf(2)) {
				parameter = Expression();
				parameters.add(parameter); 
				while (la.kind == 13) {
					Get();
					parameter = Expression();
					parameters.add(parameter); 
				}
			}
			Expect(12);
			result = new Call(variable.getReceiver(), variable.getName(), parameters); 
		}
		if (la.kind == 14) {
			Get();
			result = MemberExpression(result);
		}
		return result;
	}



    private DSLExpression parseImpl() {
        la = new Token();
        la.val = "";
        Get();
        DSLExpression result = 		Expression();
		Expect(0);

        return result;
    }

    private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_x,_x,_x, _T,_T,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x},
		{_x,_T,_T,_x, _x,_x,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x}

    };

    public static DSLExpression parse(InputStream input) {
        Parser parser = new Parser(input);
        DSLExpression result = parser.parseImpl();
        if (parser.errors.errors.size() > 0) {
            StringBuilder msg = new StringBuilder();
            for (String error : parser.errors.errors) {
                msg.append(error).append("\n");
            }
            throw new InvalidExpressionException(msg.toString());
        }
        return result;
    }

    public static DSLExpression parse(String s) {
        return parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }
} // end Parser

class Errors {

    protected final List<String> errors = new ArrayList<>();
    public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text

    protected void printMsg(int line, int column, String msg) {
        StringBuffer b = new StringBuffer(errMsgFormat);
        int pos = b.indexOf("{0}");
        if (pos >= 0) {
            b.delete(pos, pos + 3);
            b.insert(pos, line);
        }
        pos = b.indexOf("{1}");
        if (pos >= 0) {
            b.delete(pos, pos + 3);
            b.insert(pos, column);
        }
        pos = b.indexOf("{2}");
        if (pos >= 0)
            b.replace(pos, pos + 3, msg);
        errors.add(b.toString());
    }

    public void SynErr(int line, int col, int n) {
        String s;
        switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "identifier expected"; break;
			case 2: s = "numericLiteral expected"; break;
			case 3: s = "\"||\" expected"; break;
			case 4: s = "\"<\" expected"; break;
			case 5: s = "\"<=\" expected"; break;
			case 6: s = "\">\" expected"; break;
			case 7: s = "\">=\" expected"; break;
			case 8: s = "\"==\" expected"; break;
			case 9: s = "\"!=\" expected"; break;
			case 10: s = "\"!\" expected"; break;
			case 11: s = "\"(\" expected"; break;
			case 12: s = "\")\" expected"; break;
			case 13: s = "\",\" expected"; break;
			case 14: s = "\".\" expected"; break;
			case 15: s = "??? expected"; break;
			case 16: s = "invalid Factor"; break;
            default:
                s = "error " + n;
                break;
        }
        printMsg(line, col, s);
    }

    public void SemErr(int line, int col, String s) {
        printMsg(line, col, s);
    }

    public void SemErr(String s) {
        errors.add(s);
    }

    public void Warning(int line, int col, String s) {
        printMsg(line, col, s);
    }

    public void Warning(String s) {
        errors.add(s);
    }
} // Errors

class FatalError extends RuntimeException {

    public static final long serialVersionUID = 1L;

    public FatalError(String s) {
        super(s);
    }
}
