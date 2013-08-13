package MML;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

/**
 * Micro Matrix Language Parser
 * 
 * Current implementation is just a proof of concept fot the beta release, it can have bugs and limited usability. The whole class will be rewritten 
 * before the 1.0 version of the software.
 * 
 * It currently uses simple rule-based + ONP parser instead of true LL(1) grammar parsing
 * @author lejlot
 */
public class Parser {
    /**
     *  A + B
     *  A - B
     *  A * B
     *  A .* B
     *  A / B
     *  A ./ B
     *  A ^ B
     *  A .^ B
     *  A | B
     *  A _ B
     *  A == B
     *  A <= B
     *  A >= B
     *  A < B
     *  A > B
     *  A != B
     *  A or B
     *  A and B
     *  not(A)
     *  max, min, mean, sum, count, size, sqrt, zero, ident, conv2, imconv, abs, vectorize,...
     */
    private String operators [] = { "\\*_", "_", "\\.%", "%", "\\*\\|", "\\|", "\\.\\^","\\^","\\.\\*", "\\./","\\+","-","\\(","\\)","/","'","\\*",",","#",";",">=","<=","==","\\!=","\\!","<",">","and","or","not", ":"};
    private HashMap<String, Integer> priority;
    private HashSet<String> funcs;
    private HashSet<String> variables;
    private HashMap<String, String> constants;
    private HashMap<String, Integer> constVals;
    
    {
        constants=new HashMap();
        constants.put("pi", "Math.PI");
        constants.put("e", "Math.E");
        
        constVals=new HashMap();
        constVals.put("ZERO",0);
        constVals.put("ONE",1);
        constVals.put("TWO",2);
        
        variables = new HashSet();
        priority = new HashMap();
        priority.put("and",11);
        priority.put("or",11);
        priority.put("not",11);
        priority.put("==",10);
        priority.put("!=",10);
        priority.put("<=",10);
        priority.put(">=",10);
        priority.put("<",10);
        priority.put(">",10);
        priority.put("+",3);
        priority.put("-",3);
        priority.put("%",2);
        priority.put(".%",2);
        priority.put("*",2);
        priority.put(".*",2);
        priority.put("/",2);
        priority.put("./",2);
        priority.put("*|",2);
        priority.put("*_",2);
        priority.put("|",2);
        priority.put("_",2);
        priority.put(".^",1);
        priority.put("^",1);
        priority.put("'",1);
        priority.put("!",1);        
        priority.put("~",1);
        priority.put(":",1);
        priority.put("$",0);        
        priority.put("#",0);        
        
        funcs = new HashSet();
        funcs.add("min");
        funcs.add("max");
        funcs.add("sum");
        funcs.add("size");
        funcs.add("mean");        
        funcs.add("count");        
        funcs.add("sqrt");        
        funcs.add("zeros");        
        funcs.add("ones");        
        funcs.add("ident");        
        funcs.add("sub");        
        funcs.add("vectorize");        
        funcs.add("inc");
        funcs.add("dec");
        funcs.add("abs");
        funcs.add("conv2");
        funcs.add("imconv");
        funcs.add("cos");
        funcs.add("sin");
        funcs.add("tg");
        funcs.add("ctg");
        funcs.add("exp");
        
    }
    
    private String spaces(String code){
        String output = code;;
        for (String operator : operators){            
            if (!operator.matches("[a-z]+"))
             output = output.replaceAll(operator," "+operator+" ");             
        }
        
        output = output.replaceAll("(\\s)+", " ");
        
        output = output.
                replaceAll("\\["," \\[ ").
                replaceAll("\\]"," \\] ").
                replaceAll("="," = ").
                replaceAll("<\\s+=","<=").
                replaceAll(">\\s+=",">=").
                replaceAll("=\\s+=","==").
                replaceAll("\\!\\s+=","\\!=").
                replaceAll("\\*\\s+_","\\*_").
                replaceAll("\\*\\s+\\|","\\*\\|").
                replaceAll("\\.\\s+\\*","\\.\\*").
                replaceAll("\\.\\s+/","\\./").
                replaceAll("\\.\\s+\\^","\\.\\^").
                replaceAll("\\.\\s%","\\.%");
        
        return output;
    }
    
    private String[] preprocess(String[] code) throws Exception{
        ArrayList<String> processed = new ArrayList();
        
        // Processing "[" and "]" characters to determine whichever are they matrices or indices
        for (int i=0; i<code.length; ++i){
            String now = code[i];
            if (now.equals("[")){
                if (i==0 || code[i-1].equals("(") || isOperator(code[i-1])){
                    processed.add("#");
                    processed.add("(");
                }else{
                    processed.add("$");
                    processed.add("(");
                }
            }else
            if (now.equals("]")){
                processed.add(")");
            }else
                processed.add(now);
        }
        
        // Const matrices procesing
        String[] first = processed.toArray(new String[0]);
        processed.clear();
        int rows=0, cols=0, end = -1, sum = 0;
        for (int i=0; i<first.length; ++i){
            String now = first[i];
            if (i==end){
                processed.add(",");
                processed.add("?"+cols+""); // number of rows for the const matrix constructor
                processed.add(",");
                processed.add("?"+rows+""); // number of columns for the const matrix consrtuctor
            }
            if (now.equals("#")){   // constructor command
                int open = 0;
                rows = 1; cols = 1; sum = 0;
                for (int j=i+1; j<first.length; ++j){
                    if (first[j].equals("(")) ++open;
                    if (first[j].equals(")")) --open;
                    if (open==0){
                        end = j;
                        if ((sum + rows )!=rows*cols) throw new Exception("Incorrect constant matrix declatation!");
                        break;
                    }
                    if (open==1){
                        if (first[j].equals(",")) { if (rows==1) ++cols; ++sum; }
                        if (first[j].equals(";")) ++rows;
                    }
                }
                processed.add("#");                
            }
            else processed.add(now.equals(";") ? "," : now);
        }
        
        // Functions arity processing
        first = processed.toArray(new String[0]);
        processed.clear();
        int ar;

        for (int i=0; i<first.length; ++i){
            String now = first[i];
            if (isFunc(now)){
                int open = 0;
                ar = 1;
                for (int j=i+1; j<first.length; ++j){
                    if (first[j].equals("(")) ++open;
                    if (first[j].equals(")")) --open;
                    if (open==0){
                        break;
                    }
                    if (open==1){
                        if (first[j].equals(",")) ++ar;
                    }
                }
                processed.add(now+"??"+ar);                
            }
            else processed.add(now);
        }
        return processed.toArray(new String[0]);
    }
    
    public String[] tokenize(String str){
        Scanner sc = new Scanner(spaces(str));
        ArrayList<String> text = new ArrayList();
        while (sc.hasNext()){
            text.add(sc.next());
        }
        return text.toArray(new String[text.size()]);
    }
    
    private String parseMMLtoJava(String code) throws Exception{
        return (toJavaCode(ONP(preprocess(tokenize(code)))));
    }
    
    private String postProcess(String code){
        
        for(String token : constVals.keySet()){
            code=code.replaceAll("\\(MathData."+token+"\\).toInt\\(\\)", ""+constVals.get(token));
            code=code.replaceAll("\\(MathData."+token+"\\).toFloat\\(\\)", constVals.get(token)+".0f");
            
            code=code.replaceAll("\\(MathData."+token+"\\).negate\\(\\).toInt\\(\\)", "-"+constVals.get(token));
            code=code.replaceAll("\\(MathData."+token+"\\).negate\\(\\).toFloat\\(\\)", "-"+constVals.get(token)+".0f");            
        }
        
        code=code.replaceAll("\\(new MathData\\(([0-9]+)\\)\\).toInt\\(\\)", "$1");
        code=code.replaceAll("\\(new MathData\\(([0-9]+\\.[0-9]+f)\\)\\).toFloat\\(\\)", "$1");

        code=code.replaceAll("\\(new MathData\\(([0-9]+)\\)\\).negate\\(\\).toInt\\(\\)", "-$1");
        code=code.replaceAll("\\(new MathData\\(([0-9]+\\.[0-9]+f)\\)\\).negate\\(\\).toFloat\\(\\)", "-$1");

        code=code.replaceAll("\\(new MathData\\(([0-9]+)\\.[0-9]+f\\)\\).toInt\\(\\)", "$1");
        code=code.replaceAll("\\(new MathData\\(([0-9]+)\\.[0-9]+f\\)\\).negate\\(\\).toInt\\(\\)", "-$1");

         return code;
    }
    
    private String processClause(String clause) throws Exception{
        String[] parts = breakCondition(clause);
        if (parts[2].trim().length()<=1 ){
            return (parts[0]+" ("+parseMMLtoJava(parts[1]) + ".toBoolean()) "+ parts[2].trim());
        }else{
            return (parts[0]+" ("+parseMMLtoJava(parts[1]) + ".toBoolean()) "+parseMMLtoJava(parts[2]));
        }        
    }
    
    private String processFor(String code) throws Exception{        
        String[] parts = breakFor(code);
        return "for (MathData "+parts[0]+"=new MathData(" + parseMMLtoJava(parts[1])+"); " + parts[5] +"("+ parts[0] + "," + parseMMLtoJava(parts[2]) + ").toBoolean(); "+parts[4]+"("+parts[0]+")) " + parts[3];
    }
    
    private String processLine(String line) throws Exception{
        if (line.trim().startsWith("//")) return line; // comment
        
        int ob=0, cb=0;
        for (int i=0; i<line.length(); ++i) if (line.charAt(i)=='{') ++ob; else if (line.charAt(i)=='}') ++cb;
        if (line.length() > 1 && (cb>0 || ob>0))
        {
            boolean finished = false;
            String[] parts = new String[2*(cb+ob)+1];
            int current = 0;
            line = " " + line + " ";
            for (int i=0; i<line.length(); ++i){
                
                if (line.charAt(i)=='{' || line.charAt(i)=='}'){
                    if (!finished){ finished=true; ++current; }
                    parts[current] = line.charAt(i)+"";
                    ++current;
                }else{
                    if (parts[current]==null) parts[current]="";
                    parts[current] += line.charAt(i);
                    finished = false;
                }                
            }
            String coded = "";
            for (int i=0; i<2*(ob+cb)+1; ++i){
                coded += processLine(parts[i]);
            }
            return coded;
        }
        
        String[] tokens = tokenize(line);
        
        String variable =null;
        int coordinates = 0;
        String coord1="", coord2="";
        int open = 0;
        
        for (int i=0; i<tokens.length; ++i){
            String token = tokens[i];
            
            
            if (token.equals("for")) return processFor(line);
            
            if (token.equals("if") || token.equals("while")) return processClause(line);
            
            if (token.equals("elseif")) return processClause(line.replace("elseif", "else if"));
            
            if (variable == null) variable = token;
            
            if (token.equals("[")) ++open;
            if (token.equals("]")) --open;
            if (open == 0){
                if (coord1.length()>0) coordinates = 1;
                if (coord2.length()>0) coordinates = 2;
            }
            if (open >= 1 && coordinates == 0 && (!token.equals("[") || coord1.length() > 1)) coord1 += token + " ";
            if (open >= 1 && coordinates == 1 && (!token.equals("[") || coord2.length() > 1)) coord2 += token + " ";
            
            if (token.equals("=")){
                variables.add(variable);
                String rest = "";
                for (int j=i+1; j<tokens.length; ++j) rest += tokens[j] + " ";
                switch (coordinates){
                // A = ONP                
                    case 0: 
                        String parsed = parseMMLtoJava(rest);
                        
                        boolean function = false;
                        for(String funcName:funcs){
                            if (parsed.startsWith(funcName+"(")){
                                function =true;
                                break;
                            }
                        }
                        if (function || parsed.startsWith("new ") || (parsed.contains(".") && !parsed.startsWith("(MathData.")) )
                            return variable+" = "+parsed+";";
                        else
                            return variable + " = new MathData("+parsed+");";
                // A[ ONP ] = ONP
                    case 1: return variable+".set("+parseMMLtoJava(coord1)+".toInt(),"+parseMMLtoJava(rest)+");";
                // A[ ONP ][ ONP ] = ONP
                    case 2: return variable+".set("+parseMMLtoJava(coord1)+".toInt(),"+parseMMLtoJava(coord2)+".toInt(),"+parseMMLtoJava(rest)+");";                    
                }
            }
        }
        return line;
    }
    
    /**
     * Main method, performing translation from MML to Java Code using MathData objects
     * @param code multi-line script in MML
     * @param debug  set it to true to include debug information
     * @param filename  name of the file used in debug information
     * @return Java equivalent of given MML script
     */
    public String parse(String code, boolean debug, String filename) throws Exception{
        Scanner sc = new Scanner(code);
        String java = "";
        int line = 0;
        if (debug) java = "try { ";
        while (sc.hasNext()){
            try {
                ++line;
                if (debug) java += "MathData.setLineNumber("+line+", \""+filename+"\");\n";
                java += postProcess(processLine(sc.nextLine()))+"\n";                
            }catch(Exception e){
                throw new Exception("Parse error in "+filename+" in line "+line);
            }
        }
        if (debug) java += "}catch(Exception e){ throw new Exception(\"Line \"+MathData.getLineNumber(\""+filename+"\") + \" in "+filename+"\"+\" \\n\"+e.getMessage() ); }";
        return java;
    }
    
    /**
     * Main method, performing translation from MML to Java Code using MathData objects
     * @param code multi-line script in MML
     * @param debug  set it to true to include debug information 
     * @return Java equivalent of given MML script
     */
    public String parse(String code, boolean debug) throws Exception{
        return parse(code, debug, null);
    }

    /**
     * Main method, performing translation from MML to Java Code using MathData objects, without debug
     * @return Java equivalent of given MML script
     */
    public String parse(String code) throws Exception{
        return parse(code, false, null);
    }
    
    /**
     * Returns all declared variables from the code
     * @return array of variables names
     */
    public String[] getVariables(){
        return variables.toArray(new String[variables.size()]);
    }
        
    private boolean isFunc(String a){
        return funcs.contains(a) || a.contains("??");
    }
    
    private boolean isSeparator(String a){
        return a.equals(",") || a.equals(";");
    }
    
    private boolean isOperator(String a){
        return priority.get(a) != null;
    }
    
    private boolean isVariable(String a){
        return !(isFunc(a)||isSeparator(a)||isOperator(a)||a.equals("(")||a.equals(")"));
    }
    
    private void output(String[] onp, int out, String value){
        onp[out]=value;
    }
    
    private int getPriority(String op){
        if (priority.containsKey(op)){
            return priority.get(op);
        }
            return -1;
    }
    
    private String[] ONP(String[] infix){
        String[] onp = new String[infix.length];
        Stack<String> stack = new Stack();
        int out = 0;
        String last = "";
        for (int i=0; i<infix.length; ++i){
            
            String now = infix[i];
            
            if (isVariable(now)){
                output(onp, out++, now);
            }
            if (isFunc(now)){
                stack.push(now);
            }
            if (isSeparator(now)){
                while (!stack.peek().equals("(")){
                    output(onp, out++, stack.pop());
                }
            }
            if (isOperator(now)){
                if (now.equals("-") && (isFunc(last) || isOperator(last) || isSeparator(last) || last.length()==0)) now = "~";
                while (!stack.empty() && (!stack.peek().equals("(") && !stack.peek().equals(")")) && getPriority(stack.peek()) <= getPriority(now)){
                    output(onp, out++, stack.pop());
                }
                stack.push(now);
            }
            if (now.equals("(")){
                stack.push(now);
            }
            
            if (now.equals(")")){
                while (!stack.peek().equals("(")){
                    output(onp, out++, stack.pop());
                }                
                if (!stack.empty() && stack.peek().equals("(")) stack.pop();
            }
            if (!now.equals("(") ) last= now;
        }
        
        while (!stack.empty() && out < onp.length) output(onp, out++, stack.pop());
        
        String[] trimmed = new String[out];
        for (int i=0; i<out; ++i) trimmed[i]=onp[i];
       
        return trimmed;
    }
    
    private String toJavaCode(String[] onp){
        Stack<String> stack = new Stack();
        for (String o : onp){
            if (o.contains("??") && isFunc(o.split("\\?\\?")[0])){
                String name = o.split("\\?\\?")[0];
                int arity = Integer.parseInt(o.split("\\?\\?")[1]);
                String code = "MathData." + name +"(";
                String[] arguments = new String[arity];
                for (int i=0; i<arity; ++i){
                    arguments[i] = stack.pop();                    
                }
                for (int i=arity-1; i>=0; --i){
                    code += arguments[i];
                    if (i!=0) code += ",";
                }
                code += ")";
                stack.push(code);
            }else if(isOperator(o)){
                String r,l;
                switch (o.charAt(0)){
                    case 'a':
                        r = stack.pop(); l = stack.pop();
                        stack.push( "MathData.and("+l+","+r+")" );                            
                        break;
                    case 'o':
                        r = stack.pop(); l = stack.pop();
                        stack.push( "MathData.or("+l+","+r+")" );                            
                        break;
                    case '<':
                        r = stack.pop(); l = stack.pop();
                        if (o.length()==2){
                            stack.push( "MathData.leq("+l+","+r+")" );                            
                        }else{
                            stack.push( "MathData.le("+l+","+r+")" );                                                    
                        }
                        break;
                    case '>':
                        r = stack.pop(); l = stack.pop();
                        if (o.length()==2){
                            stack.push( "MathData.geq("+l+","+r+")" );                            
                        }else{
                            stack.push( "MathData.ge("+l+","+r+")" );                                                    
                        }
                        break;
                    case '=':
                        r = stack.pop(); l = stack.pop();
                        stack.push( "MathData.eq("+l+","+r+")" );                                                                            
                        break;
                    case '!':
                        if (o.length()==2){
                            r = stack.pop(); l = stack.pop();
                            stack.push("MathData.not(MathData.eq("+r+","+l+"))" );
                            break;
                        }
                    case 'n':
                        r = stack.pop();
                        stack.push( "MathData.not("+r+")" );
                        break;                        
                    case '+':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".add("+r+")" );
                        break;
                    case '~':
                        r = stack.pop();
                        stack.push( r+".negate()" );
                        break;
                    case '-':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".subtract("+r+")" );
                        break;
                    case '%':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".mod("+r+")" );
                        break;
                    case '*':
                        r = stack.pop(); l = stack.pop();
                        if (o.length()==1){
                            stack.push( l+".mul("+r+")" );
                        }else{
                            switch (o.charAt(1)){
                                case '|': 
                                    stack.push( l+".sideconcat("+r+".toInt())" );
                                    break;
                                case '_': 
                                    stack.push( l+".bottomconcat("+r+".toInt())" );                                    
                                    break;
                            }
                        }
                        break;
                    case '/':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".divide("+r+")" );
                        break;
                    case '^':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".pow("+r+".toInt())" );
                        break;
                    case '|':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".concat("+r+",true)" );
                        break;
                    case '_':
                        r = stack.pop(); l = stack.pop();
                        stack.push( l+".concat("+r+",false)" );
                        break;                        
                    case '.':
                        r = stack.pop(); l = stack.pop();
                        switch (o.charAt(1)){                        
                            case '^': stack.push( l+".ppow("+r+".toFloat())" ); break;
                            case '%': stack.push( l+".pmod("+r+")" ); break;
                            case '*': stack.push( l+".pmul("+r+")" ); break;
                            case '/': stack.push( l+".pdivide("+r+")" ); break;                                
                        }
                        break;
                    case '$':
                        r = stack.pop(); l = stack.pop();
                        try{
                            int value = Integer.parseInt(r);
                            stack.push( l+".get("+value+")" );
                        }catch(NumberFormatException e){
                            stack.push( l+".get("+r+".toInt())" );
                        }
                        
                        break;
                    case '\'':
                        r = stack.pop();
                        stack.push(r+".transpose()" );
                        break;                        
                    case '#':
                        r = stack.pop(); l = stack.pop();
                        
                        int rows = Integer.parseInt(r.substring(1)), cols = Integer.parseInt(l.substring(1)); // removal of "?" special char
                        String code = "new MathData("+rows+","+cols+", new float[]{";
                        String values = "";
                        for (int x=0; x<rows*cols; ++x){
                            if (x!=0) values ="," + values;
                            values = stack.pop()+".toFloat()"+values;
                        }
                        code += values;
                        stack.push( code+"})" );
                        break;
                    case ':':
                        r = stack.pop();
                        l = stack.pop();
                        stack.push( l+".to("+r+")" );
                        break;
                }
            }else{
                
                try{
                        float value = Float.parseFloat(o);
                        if (value == 0){
                            stack.push("(MathData.ZERO)");
                        }else
                        if (value == 1){
                            stack.push("(MathData.ONE)");
                        }else
                        if (value == 2){
                            stack.push("(MathData.TWO)");
                        }else
                        stack.push( "(new MathData("+value+"f))" );
                    }catch(NumberFormatException e){
                        stack.push( o );
                    }
                
            }
        }
        return stack.pop();
      
    }
    
    private String[] breakFor(String code) throws Exception{
        String[] tokens = preprocess(tokenize(code));
        String ident="", init="", limit="", end="", change="", sign = "";
        int part = -1;
        for (String token : tokens){
            if (token.equals("for")){
                part = 0;
            }else if (token.equals("=")) {
                part = 1;
            }else if (token.equals("to") || token.equals("downto")){
                part = 2;
                change = token.equals("to") ? "MathData.inc" : "MathData.dec";
                sign  =token.equals("to") ? "MathData.leq" : "MathData.geq";
            }else if (token.equals("{")){
                end = "{"; // Shouldn't ever happen due to "{" "}" line break policy
                break;
            }else{
                switch (part){
                    case 0: ident = token; break;
                    case 1: init += token + " "; break;
                    case 2: limit += token + " "; break;
                }
            }
        }
        if (ident.length() == 0 || limit.length()==0 || init.length()==0 || change.length()==0 || sign.length()==0) throw new Exception("Cannot parse the for statement");
        return new String[]{ident, init, limit, end, change, sign};        
    }
    
    private String[] breakCondition(String code) throws Exception{
        int open = 0;
        
        String precondition = "", condition="", postcondition="";
        for (int i=0; i<code.length(); ++i){
            if (code.charAt(i) == '('){
                ++open;
            }
            if (code.charAt(i) == ')'){
                --open;
            }
            if (open == 0){ 
                if (condition.length()==0)
                    precondition += code.charAt(i);
                else if (code.charAt(i)!=')' || postcondition.length()>1)
                    postcondition += code.charAt(i);
            }else{
                if (condition.length()!=0 || code.charAt(i)!='(') condition += code.charAt(i);
            }
            
        }
        if (precondition.length()==0 || condition.length()==0) throw new Exception("Cannot parse condition clause");
        return new String[]{precondition, condition, postcondition};
    }
    
    /**
     * Creats the declaration of static onstants like "pi"
     * @return Java code declaring final static MathData objects
     */
    public String getConstantsDecalaration(){        
        StringBuilder sb = new StringBuilder();
        for(Entry<String, String> cons : constants.entrySet()){
            sb.append("final MathData ").append(cons.getKey()).append("=new MathData(").append(cons.getValue()).append(");\n");
        }
        return sb.toString();
    }

    /**
     * Gets code containing both variable declarations and actual computations
     * @param code MML code
     * @return Java code
     * @throws Exception in case of incorrect MML code 
     */
    public String getWholeCode(String code) throws Exception{
        StringBuilder sb = new StringBuilder();
        sb.append(getConstantsDecalaration());
        String parsed=parse(code);
        for(String var:getVariables()) sb.append("MathData ").append(var).append(";");
        sb.append("\n");
        return sb.append(parsed).toString();
    }
    
    /**
     * Compiles provided .mml file and runs it
     * @param args array containing the path to .mml file and (optionaly) name of the ouput variable
     */
    public static void main(String[] args) {
        if (args.length==0){
            System.out.println("Usage: java -jar MMl.jar file.mml [output variable]");
            System.exit(0);
        }
        int id=Math.abs((new Random()).nextInt());
        String name="MMLTest"+id;
        try{
            Parser par = new Parser();
            String java = par.getWholeCode(Utils.load(new File(args[0])));
            String printcode;
            if(args.length>1) {
                printcode= args[1]+ ".print();";
            }else{
                printcode="";
                String[] vars = par.getVariables();
                Arrays.sort(vars);
                for(String var : vars){
                    printcode += "System.out.println(\""+var+"=\");"+var+".print();System.out.println();";
                }
            }
            java = "import MML.MathData; public class "+name+"{ public static void main(String[] args){ try{ "+java+ " "+printcode+" }catch(Exception e){ System.out.println(e.getMessage()); } }}\n";
            Utils.save(java, new File(Utils.getTempDir()+File.separator+name+".java"));
            Process p = Runtime.getRuntime().exec("javac "+Utils.getTempDir()+File.separator+name+".java -cp ./MML.jar");
            BufferedReader in = new BufferedReader( new InputStreamReader(p.getInputStream()) );
            String line;
            while ((line = in.readLine()) != null) {}
            p = Runtime.getRuntime().exec("java -cp "+Utils.getTempDir()+File.separator+":./MML.jar "+name);
            in = new BufferedReader( new InputStreamReader(p.getInputStream()) );
            while ((line = in.readLine()) != null) {
             System.out.println(line);
            }            
        }catch(Exception e){
            e.printStackTrace();
        }
        (new File(Utils.getTempDir()+File.separator+name+".java")).delete();
        (new File(Utils.getTempDir()+File.separator+name+".class")).delete();        
    }
}


