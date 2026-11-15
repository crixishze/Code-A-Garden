package codeagarden;

import java.util.*;
import java.util.regex.*;

class Lexer {
    static List<String> lex(String src){
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+|[A-Za-z_]\\w*|==|!=|<=|>=|\\+\\+|\\S").matcher(src);
        while (m.find()) tokens.add(m.group());
        tokens.add("<EOF>");
        return tokens;
    }
}
