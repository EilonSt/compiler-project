package ast;

import java.io.*;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;

public class AstSemanticChecksVisitor implements Visitor {
    private PrintWriter outFile;
    private String currClass;
    private Program prog;
    private HashMap<String,String> currClassFieldTypes = new HashMap<String,String>();
    private HashMap<String,String> currMethodFormalTypes;
    private HashMap<String,String> currMethodVarTypes;
    private HashMap<String,Boolean> currMethodVariablesInit; 
    private HashSet<String> classNames = new HashSet<String>();
    private String tmpClass;/* set during type() visits.set to "int"\"int-array"\"bool"\class_name according to the type of the object.*/

    private StringBuilder builder = new StringBuilder();

    public AstSemanticChecksVisitor(PrintWriter outfile){
        super();
        this.outFile = outfile;
    }


    public void exitProgram(){
        outFile.write("ERROR\n");
        outFile.flush();
        outFile.close();
        System.exit(0);
    }


    public String getString() {
        return builder.toString();
    }

    private void visitBinaryExpr(BinaryExpr e) {
        // 21
        e.e1().accept(this);
        if (!tmpClass.equals("int")){
            System.out.println("ERROR - e1 in mathematical operation is not an int");
            exitProgram();
        }
        e.e2().accept(this);
        if (!tmpClass.equals("int")){
            System.out.println("ERROR - e2 in mathematical operation is not an int");
            exitProgram();
        }
    }

    @Override
    public void visit(Program program) {

        prog = program;

        String mainClassName = program.mainClass().name();

        // 1 + 2 + 3
        for (ClassDecl classdecl : program.classDecls()) {

            String superName = classdecl.superName();

            String classname = classdecl.name();
            // check if a class with the same name was already declared
            if (classNames.contains(classname) || classname.equals(mainClassName)) {
                System.out.println("ERROR duplicate class name\n");
                exitProgram();
            }
            // check if class extends class that wasn't declared yet or main class
            if (superName != null && !classNames.contains(superName)) {
                System.out.println("ERROR class extends class that wasn't declared yet\n");
                exitProgram();
            }
            classNames.add(classname);
        }


        for (ClassDecl classdecl : program.classDecls()){

            String classname = classdecl.name();

            //HashMap<String, String> myFields = program.getClassFields(classname);
            HashMap<String, String> myFields = new HashMap<String, String>();
            //HashMap<String,ArrayList<String>> myMethods = classdecl.methodsSet();
            HashMap<String,ArrayList<String>> myMethods = new HashMap<String,ArrayList<String>>();
            HashSet<String> myAncestors = new HashSet<String>();

            // add pointers to program map
            program.addClassFields(classname, myFields);
            program.addClassMethods(classname, myMethods);
            program.addClassAncestors(classname, myAncestors);

            String superName = classdecl.superName();
            
            //if class has super class, copy the superclass's fields\method signatures\ancestors
            if (superName != null){
                //copy super class fields to my fields
                HashMap<String,String> superClassFields = prog.getClassFields(superName);
                for (String fieldName:superClassFields.keySet()){
                    String fieldType = superClassFields.get(fieldName);
                    myFields.put(fieldName,fieldType);
                }
                //copy super class method signatures to my method signatures
                HashMap<String,ArrayList<String>> superClassMethods = prog.getClassMethods(superName);
                for (var methodSignature : superClassMethods.values()){
                    String methodName = methodSignature.get(0);
                    myMethods.put(methodName,methodSignature);
                }
                //copy super class ancestors to my ancestors
                HashSet<String> superClassAncestors = prog.getClassAncestors(superName);
                for (String ancestorName:superClassAncestors){
                    myAncestors.add(ancestorName);
                }
                //add direct super class to my ancestors
                myAncestors.add(superName);
            }

            // 4
            //add fields while checking that there are no duplicate names
            for(var fieldDecl:classdecl.fields()){
                String fieldName = fieldDecl.name();
                if (myFields.keySet().contains(fieldName)){
                    System.out.println("ERROR duplicate field name in class" + classname +"\n");
                    exitProgram();
                }
                fieldDecl.accept(this);
                myFields.put(fieldName,tmpClass);
            }
            
            // 5
            HashSet<String> checkMethods = new HashSet<String>();
            for (var methodDecl:classdecl.methoddecls()){
                if (checkMethods.contains(methodDecl.name())){// method with the same name was already declared in this class
                    System.out.println("ERROR duplicate method name in class" + classname +"\n");
                    exitProgram();
                }
                checkMethods.add(methodDecl.name());
            }

            // 6
            for (var methodDecl:classdecl.methoddecls()){
                String methodName = methodDecl.name();
                //ArrayList<String> staticSignature = methodDecl.staticSignature();
                ArrayList<String> staticSignature = new ArrayList<String>();
                // set static signature
                staticSignature.add(methodName);
                for(var formal: methodDecl.formals()){
                    formal.accept(this);
                    staticSignature.add(tmpClass);
                }
                methodDecl.returnType().accept(this);
                staticSignature.add(tmpClass);
                if (myMethods.containsKey(methodName)){ //method with the same name exists in one of the class's ancestors

                    //compare signatures, if my signature doesn't match the ancestor's signature, print ERROR and exit
                    ArrayList<String> superMethodStaticSignature = myMethods.get(methodName);
                    methodDecl.returnType().accept(this); 
                    int mySignatureSize = staticSignature.size();
                    int superMethodSignaturesize = superMethodStaticSignature.size();
                    if (mySignatureSize!=superMethodSignaturesize){
                        System.out.println("ERROR - overriding method has different amount of formals\n");
                        exitProgram();
                    }
                    else{
                        for (int i=1;i<mySignatureSize-1;i++){
                            if (! staticSignature.get(i).equals(superMethodStaticSignature.get(i))){
                                System.out.println("ERROR - overriding method has different types of formals\n");
                                exitProgram();
                            }
                        }
                        String myMethodRetType = staticSignature.get(mySignatureSize-1);
                        String superMethodRetType = superMethodStaticSignature.get(superMethodSignaturesize-1);
                        if (!(myMethodRetType.equals(superMethodRetType))){
                            //check if my method ret type extends super method ret type
                            if (myMethodRetType.equals("int") || myMethodRetType.equals("int-array") || myMethodRetType.equals("bool")){
                                System.out.println("ERROR - overriding method has different return type");
                                exitProgram();
                            }
                            if (!prog.getClassAncestors(myMethodRetType).contains(superMethodRetType)){
                                System.out.println("ERROR - overriding method has different return type");
                                exitProgram();
                            }
                        }
                    }
                    
                }
                else{                    
                    myMethods.put(methodName,staticSignature);
                }            
 
            }

        }

        for (ClassDecl classdecl : program.classDecls()){
            classdecl.accept(this);
        }

        program.mainClass().accept(this);
    }

    @Override
    public void visit(ClassDecl classDecl) {

        currClass = classDecl.name();
        currClassFieldTypes = prog.getClassFields(currClass);

        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {

        String classname = mainClass.name();
        currClass = classname;

        HashMap<String, String> myFields = new HashMap<String, String>();
        HashMap<String,ArrayList<String>> myMethods = new HashMap<String,ArrayList<String>>();
        HashSet<String> myAncestors = new HashSet<String>();

        // main method has no methods(other than main)\formals\fields\vars, so all of these maps are empty.
        // created them just so that when searching for an identifier in other visits we don't get a
        // null pointer exception because the maps don't exist
        prog.addClassFields(classname, myFields);
        prog.addClassMethods(classname, myMethods);
        prog.addClassAncestors(classname, myAncestors);

        currClassFieldTypes = myFields;
        currMethodFormalTypes = new HashMap<String,String>();
        currMethodVarTypes = new HashMap<String,String>();
        currMethodVariablesInit = new HashMap<String,Boolean>();

        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {

        HashMap<String,Boolean> methodVariablesInit = new HashMap<String,Boolean>();
        currMethodVariablesInit = methodVariablesInit;
        HashMap<String,String> methodVarTypes = new HashMap<String,String>();
        currMethodVarTypes = methodVarTypes;
        HashMap<String,String> methodFormalTypes = new HashMap<String,String>();
        currMethodFormalTypes = methodFormalTypes;

        // 24
        // check that there are no duplicate names in variables and formals declerations
        for (var varDecl : methodDecl.vardecls()) {
            String varName = varDecl.name();
            if (methodVarTypes.containsKey(varName)){
                System.out.println("ERROR duplicate variable name in method " + methodDecl.name());
                exitProgram();
            }
            methodVariablesInit.put(varName,false);
            varDecl.accept(this);
            methodVarTypes.put(varName,tmpClass);
        }
        for (var formal:methodDecl.formals()){
            String formalName = formal.name();
            if (methodFormalTypes.containsKey(formalName) || methodVarTypes.containsKey(formalName)){
                System.out.println("ERROR duplicate formal name in method " + methodDecl.name());
                exitProgram();
            }
            formal.accept(this);
            methodFormalTypes.put(formalName,tmpClass);
        }

        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        // 18
        ArrayList<String> staticSignature = prog.getClassMethodByName(currClass,methodDecl.name());
        String methodReturnType = staticSignature.get(staticSignature.size()-1);        
        methodDecl.ret().accept(this);
        String retType = tmpClass;
        if (!methodReturnType.equals(retType)){
            //maybe retType is a subtype of MethodReturnType
            if (methodReturnType.equals("int") || methodReturnType.equals("bool") || methodReturnType.equals("int-array")){
                System.out.println("ERROR method return type does not match type returned (18)");
                exitProgram();
            }
            else if (retType.equals("int") || retType.equals("bool") || retType.equals("int-array")){
                System.out.println("ERROR method return type does not match type returned (18)");
                exitProgram();
            }
            else{ // both types are classes. check if right extends left
                if (!(prog.getClassAncestors(retType).contains(methodReturnType))){
                    System.out.println("ERROR method return type does not match type returned (18)");
                    exitProgram();
                }
            }
        }

    }

    @Override
    public void visit(FormalArg formalArg) {
        formalArg.type().accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type().accept(this); 
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    public void copyHashMap(HashMap<String,Boolean> from,HashMap<String,Boolean> to){
        for (String variableName:from.keySet()){
            Boolean isInit = from.get(variableName);
            to.put(variableName,isInit);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        HashMap<String,Boolean> thenCase = new HashMap<String,Boolean>();
        HashMap<String,Boolean> elseCase = new HashMap<String,Boolean>();
        copyHashMap(currMethodVariablesInit,thenCase);
        copyHashMap(currMethodVariablesInit,elseCase);

        HashMap<String,Boolean> prevMethodVariablesInit = currMethodVariablesInit;

        ifStatement.cond().accept(this);

        // 17
        if(!tmpClass.equals("bool")){
            System.out.println("ERROR - if condition expr isn't a bool");
            exitProgram();
        }

        currMethodVariablesInit = thenCase;
        ifStatement.thencase().accept(this);
        
        currMethodVariablesInit = elseCase;
        ifStatement.elsecase().accept(this);
        
        currMethodVariablesInit = prevMethodVariablesInit;

        for(String variableName:thenCase.keySet()){
            if (!currMethodVariablesInit.get(variableName)){
                Boolean isInitThenCase = thenCase.get(variableName);
                Boolean isInitElseCase = elseCase.get(variableName);
                if (isInitElseCase && isInitThenCase){
                    currMethodVariablesInit.put(variableName,true);
                }
            }
        }
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        HashMap<String,Boolean> whileVariablesInit = new HashMap<String,Boolean>();
        copyHashMap(currMethodVariablesInit,whileVariablesInit);
        
        HashMap<String,Boolean> prevMethodVariablesInit = currMethodVariablesInit;

        whileStatement.cond().accept(this);

        // 17
        if(!tmpClass.equals("bool")){
            System.out.println("ERROR - while condition expr isn't a bool");
            exitProgram();
        }

        currMethodVariablesInit = whileVariablesInit;

        whileStatement.body().accept(this);

        currMethodVariablesInit = prevMethodVariablesInit;
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
        // 20
        if (!tmpClass.equals("int")){
            System.out.println("ERROR - sysout argument is not an int");
            exitProgram();
        }
    }

    @Override
    public void visit(AssignStatement assignStatement) {

        String leftValueName = assignStatement.lv();
        String leftValueType = "";
        // look for left Value Type in hashmaps
        if (currMethodVarTypes.containsKey(leftValueName)){
            leftValueType = currMethodVarTypes.get(leftValueName);
        }
        else if (currMethodFormalTypes.containsKey(leftValueName)){
            leftValueType = currMethodFormalTypes.get(leftValueName);
        }
        else if (currClassFieldTypes.containsKey(leftValueName)){
            leftValueType = currClassFieldTypes.get(leftValueName);
        }
        // 14
        else{
            System.out.println("ERROR - left value does not exist");
            exitProgram();
        }
        
        assignStatement.rv().accept(this);
        String rightValueType = tmpClass;

        // 16
        if(!rightValueType.equals(leftValueType)){
            //maybe subtype
            if (rightValueType.equals("int") || rightValueType.equals("bool") || rightValueType.equals("int-array")){
                System.out.println("ERROR semantic check 16");
                exitProgram();
            }
            else if (leftValueType.equals("int") || leftValueType.equals("bool") || leftValueType.equals("int-array")){
                System.out.println("ERROR semantic check 16");
                exitProgram();
            }
            else{ // both types are classes. check if right extends left
                if (!(prog.getClassAncestors(rightValueType).contains(leftValueType))){
                    System.out.println("ERROR semantic check 16 - left value is not an ancestor of right value");
                    exitProgram();
                }
            }

        }

        // 15 
        if (currMethodVariablesInit.keySet().contains(leftValueName)){ // lv is a variable and it is currently being initialized
            currMethodVariablesInit.put(leftValueName,true);
        }
        

    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {

        String arrayName = assignArrayStatement.lv();
        String leftValueType = "";

        // look for array Type in hashmaps
        if (currMethodVarTypes.containsKey(arrayName)){
            leftValueType = currMethodVarTypes.get(arrayName);
        }
        else if (currMethodFormalTypes.containsKey(arrayName)){
            leftValueType = currMethodFormalTypes.get(arrayName);
        }
        else if (currClassFieldTypes.containsKey(arrayName)){
            leftValueType = currClassFieldTypes.get(arrayName);
        }
        else{
            System.out.println("ERROR - array does not exist");
            exitProgram();
        }

        // 23
        if (!leftValueType.equals("int-array")){
            System.out.println("ERROR - array assignment called on object that is not an array");
            exitProgram();
        }

        // 15
        if (currMethodVariablesInit.keySet().contains(arrayName)){ // lv is a variable and it is currently being initialized
            if(currMethodVariablesInit.get(arrayName) == false){
                System.out.println("ERROR array assigned before init");
                exitProgram();
            }
        }

        // 23
        assignArrayStatement.index().accept(this);

        if (!tmpClass.equals("int")){
            System.out.println("ERROR assign array - array index is not an int");
            exitProgram();
        }

        assignArrayStatement.rv().accept(this);

        if (!tmpClass.equals("int")){
            System.out.println("ERROR assign array - right value is not an int");
            exitProgram();
        }
    }

    @Override
    public void visit(AndExpr e) {//ALREADY MERGED
        // 21
        e.e1().accept(this);
        if (!tmpClass.equals("bool")){
            System.out.println("ERROR - e1 in and expression is not an bool");
            exitProgram();
        }
        e.e2().accept(this);
        if (!tmpClass.equals("bool")){
            System.out.println("ERROR - e2 in and expression is not an bool");
            exitProgram();
        }

        tmpClass="bool";
    }

    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e);
        tmpClass = "bool";
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e);
        tmpClass = "int";
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e);
        tmpClass="int";
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e);
        tmpClass="int";
    }

    @Override
    public void visit(ArrayAccessExpr e) {

        e.arrayExpr().accept(this); // checks that array is initiliazed (15)
        // 22
        if (!tmpClass.equals("int-array")){
            System.out.println("ERROR - array access object is not array");
            exitProgram();
        }

        e.indexExpr().accept(this);
        // 22
        if (!tmpClass.equals("int")){
            System.out.println("ERROR - array access index is not an int");
            exitProgram();
        }

        tmpClass = "int";
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        // 13
        String objectClassName = e.arrayExpr().getClass().getName();
        if(objectClassName.equals("ast.IdentifierExpr")){
            e.arrayExpr().accept(this); // sets the tmpClass 
            if (!tmpClass.equals("int-array")){
                System.out.println("ERROR length expression called not on int-array\n");
                exitProgram();
            }
        }
        else if (objectClassName.equals("ast.NewIntArrayExpr")){
            e.arrayExpr().accept(this);
        }
        else if (objectClassName.equals("ast.MethodCallExpr")){
            e.arrayExpr().accept(this);
            if(!tmpClass.equals("int-array")){//return type of method is not array
                System.out.println("ERROR length expression called not on int-array\n");
                exitProgram();
            }            
        }
        else{
            System.out.println("ERROR length called on an object that is not an array\n");
            exitProgram();
        }
        tmpClass = "int";

    }

    @Override
    public void visit(MethodCallExpr e) {
        String ownerExpr_type = e.ownerExpr().getClass().getName();
        String methodName = e.methodId();
        // 12 
        if (!(ownerExpr_type.equals("ast.IdentifierExpr")||ownerExpr_type.equals("ast.NewObjectExpr")||ownerExpr_type.equals("ast.ThisExpr"))){
            System.out.println("ERROR method invocation not on identifier or new or ownereobj");
            exitProgram();
        }

        e.ownerExpr().accept(this);
        String methodClass = tmpClass;

        // 10
        if (methodClass.equals("int") || methodClass.equals("bool") || methodClass.equals("int-array")){
            System.out.println("ERROR method called on int or bool or int-array\n");
            exitProgram();
        }

        // 11
        ArrayList<String> methodStaticSignature = prog.getClassMethodByName(methodClass,methodName);
        if (methodStaticSignature == null){
            System.out.println("ERROR method does not exist for this class");
            exitProgram();
        }
        //check if num formals equals num actuals
        if(e.actuals().size() != (methodStaticSignature.size()-2)){
            System.out.println("ERROR number of formals and number of actuals do not match");
            exitProgram();
        }
        // check if actual types match formal types
        int cnt = 1;
        for (Expr arg : e.actuals()) {
            arg.accept(this);
            String methodFormalType = methodStaticSignature.get(cnt);
            if(!tmpClass.equals(methodFormalType)){
                if (methodFormalType.equals("bool") || methodFormalType.equals("int") || methodFormalType.equals("int-array")){
                    System.out.println("ERROR actual doesn't match formal type\n");
                    exitProgram();
                }
                else if (tmpClass.equals("bool") || tmpClass.equals("int") || tmpClass.equals("int-array")){
                    System.out.println("ERROR actual doesn't match formal type\n");
                    exitProgram();
                }
                else {// check if actual type extends the formal type
                    HashSet<String> ancestors = prog.getClassAncestors(tmpClass);
                    if (!ancestors.contains(methodFormalType)){
                        System.out.println("ERROR actual class doesn't extend formal class\n");
                        exitProgram();
                    }
                }
            }
            cnt++;
        }

        String methodReturnType = methodStaticSignature.get(methodStaticSignature.size()-1); 
        
        tmpClass = methodReturnType;
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        tmpClass = "int";
    }

    @Override
    public void visit(TrueExpr e) {
        tmpClass = "bool";
    }

    @Override
    public void visit(FalseExpr e) {
        tmpClass = "bool";
    }

    @Override
    public void visit(IdentifierExpr e) {

        String identifier = e.id();
        boolean identifierIsInitialized = false; 

        // 15
        if (currMethodVariablesInit.containsKey(identifier)){ // identifier is a variable, so we need to confirm it's initialized
            identifierIsInitialized = currMethodVariablesInit.get(identifier);
            if (!identifierIsInitialized){
                System.out.println("ERROR identifier values is being used but it is not initialized");
                exitProgram();
            }
        }

        String identifierType = "";
        // look for identifier in hashmaps
        if (currMethodVarTypes.containsKey(identifier)){
            identifierType = currMethodVarTypes.get(identifier);
        }
        else if (currMethodFormalTypes.containsKey(identifier)){
            identifierType = currMethodFormalTypes.get(identifier);
        }
        else if (currClassFieldTypes.containsKey(identifier)){
            identifierType = currClassFieldTypes.get(identifier);
        }
        // 14
        else{
            System.out.println("ERROR - identifier does not exist");
            exitProgram();
        }

        tmpClass = identifierType;
        
    }

    public void visit(ThisExpr e) {
        tmpClass=currClass;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        // 25
        e.lengthExpr().accept(this);
        if(!tmpClass.equals("int")){
            System.out.println("ERROR new int array length is not an int");
            exitProgram();
        }
        tmpClass = "int-array";
    }

    @Override
    public void visit(NewObjectExpr e) {
        String refName = e.classId();
        // 9
        if(!classNames.contains(refName)){
            System.out.println("ERROR new object class doesnt exist\n");
            exitProgram();
        }
        tmpClass = refName;
    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
        // 21
        if (!(tmpClass.equals("bool"))){
            System.out.println("ERROR - not expression on expr that is not a boolean");
            exitProgram();
        }
        tmpClass = "bool";
    }

    @Override
    public void visit(IntAstType t) {
        tmpClass = "int";
    }

    @Override
    public void visit(BoolAstType t) {
        tmpClass = "bool";
    }

    @Override
    public void visit(IntArrayAstType t) {
        tmpClass = "int-array";
    }

    @Override
    public void visit(RefType t) {
        String refName =t.id();
        // 8 - decleration is of an exisitng class
        if(!classNames.contains(refName)){
            System.out.println("ERROR ref id class not exists\n");
            exitProgram();
        }
        tmpClass = refName;
    }

}
