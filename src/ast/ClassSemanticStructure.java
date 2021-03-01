package ast;

import java.util.ArrayList;

public class ClassSemanticStructure {
    private ArrayList<String> fieldsNames;
    private ArrayList<String> methodsNames;

    public ArrayList<String> fieldsNames(){
        return this.fieldsNames;
    }
    
    public ArrayList<String> methodsNames(){
        return this.methodsNames;
    }
    
}