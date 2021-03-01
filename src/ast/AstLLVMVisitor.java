package ast;

import java.util.ArrayList;

/*Visitor that translates code from xml to LLVM and prints it to an output file*/
public class AstLLVMVisitor implements Visitor {
    private StringBuilder builder = new StringBuilder();
    private long regcnt = 0; 
    private int indent = 1;
    private ClassDecl curr_class;
    private int ifCount = 0;
    private int loopCount = 0;
    private int andCount = 0;
    private int arrallocCount= 0;
    private Program prog;
    private String tmpClass;/*set during type() visits.set to "int"\"int-array"\"bool"\class_name according to the type of the object.*/
    private String classOfMethodCall;/*for methodCallExpr visitor. set during identifierExpr and newObjectExpr visits.*/
    private SymbolTable currST;/*points to the ST of the enclosing scope of our current location in the program*/
    private VTFields currVTFields; /*points to the VTFields of the class we are currently in. gets modified in ClassDecl visit*/

    /*Called when accessing a field. Loads the field from the object's vtable*/
    private void printFieldLoad(String fieldName){
        int startIndex = currVTFields.getStartIndexByName(fieldName);
        appendWithIndent("%_" + regcnt + " = getelementptr i8, i8* %this, i32 " + startIndex);
        regcnt++;
        builder.append("\n");
        String identifierClass = currST.getIdentifiersClass(fieldName);
        String obj_LLVM_type = getLLVMType(identifierClass);
        appendWithIndent("%_" + regcnt + " = bitcast i8* %_" + (regcnt-1) + " to " + obj_LLVM_type + "*");
        regcnt++;
        builder.append("\n");
    }

    /*gets a string and translates it from minijava type to LLVM type. returns the translation*/
    public String getLLVMType(String type){
        String ret = null;
        switch (type) {
            case "int-literal":
                ret = "i32";
                break;
            case "int":
                ret = "i32";
                break;
            case "int-array":
                ret = "i32*";
                break;
            case "bool":
                ret = "i1";
                break;
            case "ref":
                ret = "i8*";
                break;
            default:// probably its a ref but type is ref.id
                ret = "i8*";
        }
        return ret;
    }

    public String getString() {
        return builder.toString();
    }

    private void appendWithIndent(String str) {
        builder.append("\t".repeat(indent));
        builder.append(str);
    }

    /*prints the classes vtables created by the AstVTVisitor to the output file*/
    private void addClassVTs(Program program){
        for (ClassDecl classDecl : program.classDecls()) {
            /*Add class to (className,pointer to class's VT) hashmap*/
            String className = classDecl.name();
            VTFields classVT = classDecl.VTF;
            program.addClassVTFields(className, classVT);

            //print class vtables at begining of output file
            VTMethods VTM = classDecl.VTF.getVT();
            int numOfMethods=VTM.getNumOfMethods();
            builder.append("@.");
            builder.append(classDecl.name());
            builder.append("_vtable = global [");
            builder.append(numOfMethods);
            builder.append(" x i8*] [");
            if (numOfMethods == 0){
                builder.append("]");
            }
            else if (numOfMethods == 1){
                builder.append("i8* bitcast (");
                VTMethodsEntry thisMethodEntry = VTM.EntryAtIndex(0);
                String ret_type = getLLVMType(thisMethodEntry.getRetType());
                builder.append(ret_type);
                builder.append(" (i8*");
                ArrayList<String> thisMethodParams = thisMethodEntry.getParams();
                for(int j = 0; j < thisMethodParams.size(); j++){
                    builder.append(", ");
                    String param_type = getLLVMType(thisMethodParams.get(j));
                    builder.append(param_type);
                }
                builder.append(")* ");
                builder.append("@");
                builder.append(thisMethodEntry.getClassName());
                builder.append(".");
                builder.append(thisMethodEntry.getMethodName());
                builder.append(" to i8*)]");
            }
            else{
                builder.append("\n");
                for(int i = 0;i < numOfMethods;i++){
                    appendWithIndent("i8* bitcast (");
                    VTMethodsEntry thisMethodEntry = VTM.EntryAtIndex(i);
                    String ret_type = getLLVMType(thisMethodEntry.getRetType());
                    builder.append(ret_type);
                    builder.append(" (i8*");
                    ArrayList<String> thisMethodParams = thisMethodEntry.getParams();
                    for(int j = 0; j < thisMethodParams.size(); j++){
                        builder.append(", ");
                        String param_type = getLLVMType(thisMethodParams.get(j));
                        builder.append(param_type);
                    }
                    builder.append(")* ");
                    builder.append("@");
                    builder.append(thisMethodEntry.getClassName());
                    builder.append(".");
                    builder.append(thisMethodEntry.getMethodName());
                    builder.append(" to i8*)");
                    if((i+1) != numOfMethods){
                        builder.append(",");
                    }
                    builder.append("\n");
                }
                builder.append("]");
            }
            builder.append("\n");
            builder.append("\n");
        
        }
    }

    /* parallel to java's sysout*/
    private void addPrintMethod(){
        builder.append("define void @print_int(i32 %i) {\n");
        appendWithIndent("%_str = bitcast [4 x i8]* @_cint to i8*\n");
        appendWithIndent("call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n");
        appendWithIndent("ret void\n");
        builder.append("}\n");
    }

    private void addDeclarartionsAndConstants(){
        char quotes = '"';
        builder.append("declare i8* @calloc(i32, i32)\n");
        builder.append("declare i32 @printf(i8*, ...)\n");
        builder.append("declare void @exit(i32)\n");
        builder.append("\n");
        builder.append("@_cint = constant [4 x i8] c" + quotes + "%d\\0a\\00" + quotes + "\n");
        builder.append("@_cOOB = constant [15 x i8] c" + quotes + "Out of bounds\\0a\\00" + quotes + "\n");
    }

    private void addThrowOOBMethod(){
        builder.append("define void @throw_oob() {\n");
        appendWithIndent("%_str = bitcast [15 x i8]* @_cOOB to i8*\n");
        appendWithIndent("call i32 (i8*, ...) @printf(i8* %_str)\n");
        appendWithIndent("call void @exit(i32 1)\n");
        appendWithIndent("ret void\n");
        builder.append("}\n");
    }


    @Override
    public void visit(Program program) {
        prog = program;
        addClassVTs(program); //adds the class VTs to the output file
        addDeclarartionsAndConstants();
        addPrintMethod(); //creates the sysout method and adds it to the output file
        builder.append("\n");
        addThrowOOBMethod();
        builder.append("\n");
        program.mainClass().accept(this);
        for (ClassDecl classDecl : program.classDecls()) {
            classDecl.accept(this);
        }
        
    }

    @Override
    public void visit(ClassDecl classDecl) {
        /*No need to visit the fields because they are only saved in the vtable (by the AstVTVisitor)*/
        curr_class=classDecl;
        currST = classDecl.enclosingScope();
        currVTFields = classDecl.VTF;
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
            builder.append("\n");
        }
        currST = classDecl.enclosingScope();
        builder.append("\n");
    }

    @Override
    public void visit(MainClass mainClass) {
        builder.append("\n");
        builder.append("define i32 @main() {");
        builder.append("\n");
        mainClass.mainStatement().accept(this);
        appendWithIndent("ret i32 0");
        builder.append("\n");
        builder.append("}\n");
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        ifCount = 0;
        loopCount = 0;
        andCount = 0;
        regcnt = 0;
        arrallocCount = 0;
        currST = methodDecl.enclosingScope();
        VTMethods VTM = curr_class.VTF.getVT();
        VTMethodsEntry thisMethodEntry=null;
        String ret_type="";
        for(int i=0;i<VTM.getNumOfMethods();i++){
            thisMethodEntry = VTM.EntryAtIndex(i);
            if(thisMethodEntry.getMethodName().equals(methodDecl.name())){
                ret_type=thisMethodEntry.getRetType();
                break;
            }
        }
        builder.append("define ");
        ret_type=getLLVMType(ret_type);
        builder.append(ret_type);
        builder.append(" ");
        builder.append("@");
        builder.append(curr_class.name());
        builder.append(".");
        builder.append(methodDecl.name());
        builder.append("(");
        builder.append("i8*");
        builder.append(" ");
        builder.append("%this");
        String delim = ", ";
        for (var formal : methodDecl.formals()) {
            builder.append(delim);
            formal.accept(this);
        }
        builder.append(") {\n");
        for (var formal : methodDecl.formals()) {
            formal.type().accept(this);
            String type = tmpClass;
            appendWithIndent("%");
            builder.append(formal.name());
            builder.append(" = alloca ");
            builder.append(getLLVMType(type));
            builder.append("\n");
            appendWithIndent("store ");
            builder.append(getLLVMType(type));
            builder.append(" %.");
            builder.append(formal.name());
            builder.append(", ");
            builder.append(getLLVMType(type));
            builder.append("* %");
            builder.append(formal.name());
            builder.append("\n");
        }
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        String className;
        className=methodDecl.ret().getClass().getName();

        /*Return statement*/
        if(className.equals("ast.IntegerLiteralExpr") || className.equals("ast.FalseExpr") || className.equals("ast.TrueExpr")){
            appendWithIndent("ret " + ret_type +  " ");
            methodDecl.ret().accept(this);
            builder.append("\n");
        }
        else if(className.equals("ast.ThisExpr")){
            appendWithIndent("ret " + ret_type + " ");
            builder.append("%this");
            builder.append("\n");
        }
        else{
            methodDecl.ret().accept(this);
            appendWithIndent("ret " + ret_type + " ");
            builder.append("%_");
            if(className.equals("ast.NewObjectExpr")){
                builder.append(regcnt-3);
            }
            else{
                builder.append(regcnt-1);
            }
            builder.append("\n");
        }
        builder.append("}\n");
    }

    @Override
    public void visit(FormalArg formalArg) {
        formalArg.type().accept(this);
        String type = tmpClass;
        builder.append(getLLVMType(type));
        builder.append(" %.");
        builder.append(formalArg.name());
    }

    @Override
    public void visit(VarDecl varDecl) {
        appendWithIndent("");
        varDecl.type().accept(this);
        builder.append("%");
        builder.append(varDecl.name());
        builder.append(" = alloca ");
        String type = tmpClass;
        builder.append(getLLVMType(type));
        builder.append("\n");
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
        
    }

    @Override
    public void visit(IfStatement ifStatement) {
        Object arg = ifStatement.elsecase();
        String className = arg.getClass().getName();
        if(className.equals("ast.IntegerLiteralExpr") || className.equals("ast.TrueExpr") ||className.equals("ast.TrueExpr"))
        {
            /*appendWithIndent("%_"+Integer.toString((int)regcnt)+" = load i1, i1* ");
            ifStatement.cond().accept(this);
            regcnt++;*/
            appendWithIndent("br i1 ");
            ifStatement.cond().accept(this);
        }
        else
        {
            /*condition calculate */
            ifStatement.cond().accept(this);
            //builder.append("\n");
            appendWithIndent("br i1 %_" + Integer.toString((int) regcnt - 1));
        }
        builder.append(", label %if" + Integer.toString(ifCount++));
        int Then = ifCount - 1;
        builder.append(", label %if" + Integer.toString(ifCount++) + "\n");
        int Else = ifCount - 1;
        builder.append("if" + Integer.toString(Then) + ":\n"); // new label
        /*then part*/
        ifStatement.thencase().accept(this);
        appendWithIndent("br label %if" + Integer.toString(ifCount++) + "\n");
        int End = ifCount - 1;
        builder.append("if" + Integer.toString(Else) + ":\n"); // new label
        /*else part*/
        ifStatement.elsecase().accept(this);
        appendWithIndent("br label %if" + Integer.toString(End) +"\n");
        builder.append("if" + Integer.toString(End) + ":\n"); // new label
    }


    @Override
    public void visit(WhileStatement whileStatement) {
        int firstLoop = loopCount;
        loopCount++;
        int secondLoop = loopCount;
        loopCount++;
        int thirdLoop = loopCount;
        loopCount++;
        appendWithIndent("br label %loop" + Integer.toString(firstLoop) + "\n");
        appendWithIndent("loop" + Integer.toString(firstLoop) + ":\n");
        Object arg = whileStatement.cond();
        String className = arg.getClass().getName();
        if(className.equals("ast.IntegerLiteralExpr") || className.equals("ast.TrueExpr") ||className.equals("ast.TrueExpr"))
        {
            /*appendWithIndent("%_"+Integer.toString((int)regcnt)+" = load i1, i1* ");
            ifStatement.cond().accept(this);
            regcnt++;*/
            appendWithIndent("br i1 ");
            whileStatement.cond().accept(this);
            builder.append(", label %loop" + Integer.toString(secondLoop));
        }
        else if(className.equals("ast.IdentifierExpr")){
            //appendWithIndent("%_"+Integer.toString((int)regcnt)+" = load i1, i1* ");
            whileStatement.cond().accept(this);
            //builder.append("\n");
            appendWithIndent("br i1 %_" + Integer.toString((int) regcnt - 1) + ", label %loop" + Integer.toString(secondLoop));
        }
        else{
            whileStatement.cond().accept(this);
            appendWithIndent("br i1 %_" + Integer.toString((int) regcnt - 1) + ", label %loop" + Integer.toString(secondLoop));
        }
        builder.append(", label %loop" + Integer.toString(thirdLoop) + "\n");
        appendWithIndent("loop" + Integer.toString(secondLoop) + ":\n");
        whileStatement.body().accept(this);
        appendWithIndent("br label %loop" + Integer.toString(firstLoop) +"\n");
        appendWithIndent("loop" + Integer.toString(thirdLoop) + ":\n");
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        Object arg = sysoutStatement.arg();
        String className = arg.getClass().getName();
        if(className.equals("ast.IntegerLiteralExpr")){
            appendWithIndent("call void (i32) @print_int(i32 ");
            sysoutStatement.arg().accept(this);
            builder.append(")\n");

        }
        else if(className.equals("ast.IdentifierExpr")){
            //appendWithIndent("%_"+ Integer.toString((int)regcnt++)+ " = load i32, i32* ");
            sysoutStatement.arg().accept(this);
            //builder.append("\n");
            appendWithIndent("call void (i32) @print_int(i32 %_" + Integer.toString((int)regcnt - 1));
            builder.append(")\n");
        }
        else{
            sysoutStatement.arg().accept(this);
            appendWithIndent("call void (i32) @print_int(i32 %_" + Integer.toString((int)regcnt - 1));
            builder.append(")\n");
        }
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        long regField = -1;
        long identExpr=-1;
        Object arg = assignStatement.rv();
        String className = arg.getClass().getName();
        if(!className.equals("ast.IntegerLiteralExpr") && !className.equals("ast.TrueExpr") && !className.equals("ast.FalseExpr") && !className.equals("ast.ThisExpr")){
            assignStatement.rv().accept(this);
            if(className.equals("ast.NewObjectExpr")){
                identExpr=regcnt-3;
            }
            else{
                identExpr=regcnt-1;
            }
        }
        String symbol = assignStatement.lv();
        String type = currST.getIdentifiersClass(symbol);

        if(currST.isField(assignStatement.lv())){
            printFieldLoad(assignStatement.lv());
            regField = regcnt-1;
        }
        appendWithIndent("");
        builder.append("store ");
        builder.append(getLLVMType(type));
        builder.append(" ");
        if(className.equals("ast.IntegerLiteralExpr") || className.equals("ast.TrueExpr") || className.equals("ast.FalseExpr")){
            assignStatement.rv().accept(this);
        }

        else if(className.equals("ast.NewObjectExpr")){
            builder.append("%_"+identExpr);
        }
        else if(className.equals("ast.IdentifierExpr")) {
            builder.append("%_"+identExpr);
        }
        else if(className.equals("ast.NewIntArrayExpr")){
            builder.append("%_"+identExpr);
        }
        else if(className.equals("ast.ThisExpr")){
            builder.append("%this"); 
        }
        else{
            builder.append("%_"+identExpr);
            //builder.append("%_"+Integer.toString((int)regcnt - 1));
        }
        builder.append(", ");
        builder.append(getLLVMType(type));
        builder.append("* %");

        if (regField != -1){
            builder.append("_" + regField);
        }
        else{
            builder.append(assignStatement.lv());
        }
        builder.append("\n");
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        String symbol = assignArrayStatement.lv();
        boolean field_flag=false;
        long index_reg=-1;
        long array_reg=-1;
        long elem_reg=-1;
        if(currST.isField(symbol)){
            printFieldLoad(symbol);
            field_flag=true;
        }
        appendWithIndent("%_");
        builder.append(regcnt);
        array_reg=regcnt;
        regcnt++;
        builder.append(" = ");
        builder.append("load i32*");
        builder.append(", i32** %");
        if(field_flag){
            builder.append("_"+(regcnt-2));
        }
        else{
            builder.append(assignArrayStatement.lv());
        }
        builder.append("\n");
        if(assignArrayStatement.index().getClass().getName().equals("ast.IntegerLiteralExpr")){
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" slt i32 ");
            assignArrayStatement.index().accept(this);
            builder.append(", 0");
    }
    else{
            assignArrayStatement.index().accept(this);
            index_reg=regcnt-1;
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" slt i32 ");
            builder.append("%_"+index_reg);
            builder.append(", 0");
    }
        builder.append("\n");
        appendWithIndent("br i1 %_");
        builder.append(regcnt-1);
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-2); 
        builder.append(":\n");
        appendWithIndent("call void @throw_oob()") ;
        builder.append("\n");
        appendWithIndent("br label %arr_alloc");
        builder.append(arrallocCount-1);
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-1);
        builder.append(":\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("getelementptr i32, i32* %_");
        builder.append(array_reg);
        builder.append(", i32 0");
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("load i32");
        builder.append(", i32* ");
        builder.append("%_");
        builder.append(regcnt-2);
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("icmp");
        builder.append(" sle i32 %_");
        builder.append(regcnt-2);
        builder.append(", ");
        if(assignArrayStatement.index().getClass().getName().equals("ast.IntegerLiteralExpr")){
            assignArrayStatement.index().accept(this);
        }
        else{
            builder.append("%_"+index_reg);
        }

        builder.append("\n");
        appendWithIndent("br i1 %_");
        builder.append(regcnt-1);
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-2);
        builder.append(":\n");
        appendWithIndent("call void @throw_oob()") ;
        builder.append("\n");
        appendWithIndent("br label %arr_alloc");
        builder.append(arrallocCount-1);
        builder.append("\n");
        builder.append("arr_alloc"); //new label
        builder.append(arrallocCount-1);
        builder.append(":\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("add i32 ");
        if(assignArrayStatement.index().getClass().getName().equals("ast.IntegerLiteralExpr")){
            assignArrayStatement.index().accept(this);
    }
        else{
            builder.append("%_"+index_reg);
        }
        builder.append(", 1");
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        elem_reg=regcnt;
        regcnt++;
        builder.append(" = ");
        builder.append("getelementptr i32, i32* %_");
        builder.append(array_reg);
        builder.append(", i32 %_");
        builder.append(regcnt-2);
        builder.append("\n");
        if(assignArrayStatement.rv().getClass().getName().equals("ast.IntegerLiteralExpr")){
            appendWithIndent("store i32 ");
            assignArrayStatement.rv().accept(this);
            builder.append(", i32* %_");
            builder.append(elem_reg);
    }
    //need to check if it works on arr[4] =x when x is a varibale;
    else{
        assignArrayStatement.rv().accept(this);
            appendWithIndent("store i32 %_" + (regcnt-1));
            builder.append(", i32* %_" + elem_reg);
    }
        builder.append("\n");
    }

    @Override
    public void visit(AndExpr e) {
        //long firstReg = regcnt;
        //regcnt++;
        //appendWithIndent("%_" + Integer.toString((int)firstReg) + " = load i1, i1* %");
        String className = e.e1().getClass().getName();
        int firstAnd = andCount;
        andCount ++;
        int secondAnd = andCount;
        andCount++;
        int thirdAnd = andCount;
        andCount++;
        int fourthAnd = andCount;
        andCount++;
        if(className.equals("ast.TrueExpr") || className.equals("ast.FalseExpr")){
            appendWithIndent("br label %andcond" + Integer.toString(firstAnd) + "\n");
            builder.append("andcond" + Integer.toString(firstAnd) + ":\n"); // new label
            appendWithIndent("br i1 ");
            e.e1().accept(this);
            builder.append(", label %andcond");
            builder.append(secondAnd);
            builder.append(", label %andcond");
            builder.append(fourthAnd);
            builder.append("\n");
            builder.append("andcond"); // new label
            builder.append(secondAnd);
            builder.append(":\n");
        }
        else{
            e.e1().accept(this);
            appendWithIndent("br label %andcond" + Integer.toString(firstAnd) + "\n");
            builder.append("andcond" + Integer.toString(firstAnd) + ":\n"); // new label
            appendWithIndent("br i1 %_" + Integer.toString((int)regcnt - 1) + ", label %andcond");
            builder.append(secondAnd);
            builder.append(", label %andcond");
            builder.append(fourthAnd);
            builder.append("\n");
            builder.append("andcond"); // new label
            builder.append(secondAnd);
            builder.append(":\n");
        }
        //regcnt++;
        //builder.append(secondReg);
        //builder.append(" = load i1, i1* %");
        className = e.e2().getClass().getName();
        if(className.equals("ast.TrueExpr") || className.equals("ast.FalseExpr")){
            appendWithIndent("br label %andcond");
            builder.append(thirdAnd);
            builder.append("\n");
            builder.append("andcond"); // new label
            builder.append(thirdAnd);
            builder.append(":\n");
            appendWithIndent("br label %andcond");
            builder.append(fourthAnd);
            builder.append("\n");
            builder.append("andcond"); // new label
            builder.append(fourthAnd);
            builder.append(":\n");
            appendWithIndent("%_");
            builder.append(regcnt++);
            builder.append(" = phi i1 [0, %andcond" + Integer.toString(firstAnd) + "], [");
            e.e2().accept(this);
            builder.append(", %andcond" + Integer.toString(thirdAnd) +"]\n");
        }
        else{
            e.e2().accept(this);
            long secondReg = regcnt - 1;
            appendWithIndent("br label %andcond");
            builder.append(thirdAnd);
            builder.append("\n");
            builder.append("andcond"); // new label
            builder.append(thirdAnd);
            builder.append(":\n");
            appendWithIndent("br label %andcond");
            builder.append(fourthAnd);
            builder.append("\n");
            builder.append("andcond"); // new label
            builder.append(fourthAnd);
            builder.append(":\n");
            appendWithIndent("%_");
            builder.append(regcnt++);
            builder.append(" = phi i1 [0, %andcond" + Integer.toString(firstAnd) + "], [%_");
            builder.append(Integer.toString((int)secondReg) + ", %andcond" + Integer.toString(thirdAnd) +"]\n");
        }
    }

    private void mathOperationVisit(BinaryExpr e, String cmnd){
        String first = "";
        String second = "";
        String e1_type = e.e1().getClass().getName();
        String e2_type = e.e2().getClass().getName();
        boolean e1_isIntLiteral = true;
        boolean e2_isIntLiteral = true;
        if(!e1_type.equals("ast.IntegerLiteralExpr")){
            e1_isIntLiteral = false;
            e.e1().accept(this);
            first = "%_" + (regcnt-1);
        }
        if(!e2_type.equals("ast.IntegerLiteralExpr")){
            e2_isIntLiteral = false;
            e.e2().accept(this);
            second = "%_" + (regcnt-1);
        }
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt += 1;
        builder.append(" = ");
        builder.append(cmnd);
        builder.append(" i32 ");
        if(e1_isIntLiteral){
            e.e1().accept(this);
        }
        else{
            builder.append(first);
        }
        builder.append(", ");
        if(e2_isIntLiteral){
            e.e2().accept(this);
        }
        else{
            builder.append(second);
        }
        builder.append("\n");
    }

    @Override
    public void visit(LtExpr e) {
        mathOperationVisit(e, "icmp slt");
    }

    @Override
    public void visit(AddExpr e) {
        mathOperationVisit(e, "add");
    }

    @Override
    public void visit(SubtractExpr e) {
        mathOperationVisit(e, "sub");
    }

    @Override
    public void visit(MultExpr e) {
        mathOperationVisit(e, "mul");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        long index_reg=-1;
        long array_reg=-1;
        e.arrayExpr().accept(this);
        array_reg=regcnt-1;
        if(e.indexExpr().getClass().getName().equals("ast.IntegerLiteralExpr")){
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" slt i32 ");
            e.indexExpr().accept(this);
            builder.append(", 0");
            builder.append("\n"); 
        }
        else{
            e.indexExpr().accept(this);
            index_reg=regcnt-1;
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" slt i32 %_");
            builder.append(index_reg);
            builder.append(", 0");
            builder.append("\n");

        }
        appendWithIndent("br i1 %_");
        builder.append(regcnt-1);
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-2); 
        builder.append(":\n");
        appendWithIndent("call void @throw_oob()") ;
        builder.append("\n");
        appendWithIndent("br label %arr_alloc");
        builder.append(arrallocCount-1);
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-1);
        builder.append(":\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("getelementptr i32, i32* %_");
        builder.append((array_reg));
        builder.append(", i32 0");
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("load i32");
        builder.append(", i32* ");
        builder.append("%_");
        builder.append(regcnt-2);
        builder.append("\n");
        if(e.indexExpr().getClass().getName().equals("ast.IntegerLiteralExpr")){
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" sle i32 %_");
            builder.append(regcnt-2);
            builder.append(", ");
            e.indexExpr().accept(this);
            builder.append("\n");
    }
    else{
           // e.indexExpr().accept(this);
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" sle i32 %_");
            builder.append(regcnt-2);
            builder.append(", %_");
            builder.append(index_reg);
            builder.append("\n");
    }
        appendWithIndent("br i1 %_");
        builder.append(regcnt-1);
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-2);
        builder.append(":\n");
        appendWithIndent("call void @throw_oob()") ;
        builder.append("\n");
        appendWithIndent("br label %arr_alloc");
        builder.append(arrallocCount-1);
        builder.append("\n");
        builder.append("arr_alloc"); //new label
        builder.append(arrallocCount-1);
        builder.append(":\n");
         if(e.indexExpr().getClass().getName().equals("ast.IntegerLiteralExpr")){
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("add i32 ");
            e.indexExpr().accept(this);
            builder.append(", 1");
            builder.append("\n");
    }
    else{
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("add i32 ");
            builder.append("%_"+(index_reg));
            builder.append(", 1");
            builder.append("\n");
    }
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("getelementptr i32, i32* %_");
        builder.append(array_reg);
        builder.append(", i32 %_");
        builder.append(regcnt-2);
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("load i32, i32* %_");
        builder.append(regcnt-2);
        builder.append("\n");
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        
        e.arrayExpr().accept(this);/*Always goes to IdentifierExpr or NewIntArray
        inside IdentifierExpr it does the loading of the array, depending on if the array is a field or a variable.*/

        //access the first index in the array because the array length is stored there
        appendWithIndent("%_" + regcnt + " = load i32, i32* %_" + (regcnt-1));
        regcnt++;
        builder.append("\n");
    }


    @Override
    public void visit(MethodCallExpr e) {
        String methodName = e.methodId();
        String ownerExpr_type = e.ownerExpr().getClass().getName();
        int methodIndexinVTM;
        String ownerObjectRegister;
        String methodPntrRegister;
        VTFields classVTFields;
        VTMethods classVTM;
        e.ownerExpr().accept(this);
        if (ownerExpr_type.equals("ast.IdentifierExpr")){
            ownerObjectRegister = "%_" + (regcnt-1); // because visiting IdentifierExpr loads the object pointer to regcnt - 1
            classVTFields = prog.getClassVTFields(classOfMethodCall);//classOfMethodCall depends on e.ownerExpr().accept(this);
            classVTM = classVTFields.getVT();
            methodIndexinVTM = classVTM.methodExist(methodName);  
        }
        else if (ownerExpr_type.equals("ast.NewObjectExpr")){
            ownerObjectRegister = "%_" + (regcnt-3);// because visiting NewObjectExpr loads the object pointer to regcnt - 3
            classVTFields = prog.getClassVTFields(classOfMethodCall);//classOfMethodCall depends on e.ownerExpr().accept(this);
            classVTM = classVTFields.getVT();
            methodIndexinVTM = classVTM.methodExist(methodName); 
        }
        else{ //ownerObject is "this"
            ownerObjectRegister = "%this";
            classVTFields = currVTFields;
            classVTM = classVTFields.getVT();
            methodIndexinVTM = classVTM.methodExist(methodName);
        }

        //bitcast so we can access the vtable pointer
        appendWithIndent("%_" + regcnt + " = bitcast i8* " + ownerObjectRegister + " to i8***");
        regcnt++;
        builder.append("\n");
        //load vtable pointer
        appendWithIndent("%_" + regcnt + " = load i8**, i8*** %_" + (regcnt-1));
        regcnt++;
        builder.append("\n");
        // Get a pointer to the method entry in the vtable. 
        appendWithIndent("%_" + regcnt + " = getelementptr i8*, i8** %_" + (regcnt-1) + ", i32 " + methodIndexinVTM);
        regcnt++;
        builder.append("\n");
        // Read into the array to get the actual function pointer
        appendWithIndent("%_" + regcnt + " = load i8*, i8** %_" + (regcnt-1));
        regcnt++;
        builder.append("\n");
        /* Cast the method pointer from i8* to a method ptr type that matches the function's signature*/
        String methodPtrType = classVTM.getMethodPtrType(methodIndexinVTM);
        appendWithIndent("%_" + regcnt + " = bitcast i8* %_" + (regcnt-1) + " to " + methodPtrType);
        methodPntrRegister = "%_" + regcnt;
        regcnt++;
        builder.append("\n");  
        
        //load actuals and save their registers in regNums array so we can add them to the method call.
        int numActuals = e.actuals().size(); //== numParams
        ArrayList<String> regNums = new ArrayList<String>();
        for (int i=0;i<numActuals;i++){
            Expr arg = e.actuals().get(i);
            String argType = arg.getClass().getName();
            /*Only actuals which are not IntegerLiteralExpr\FalseExpr\TrueExpr need loading.
            We will access IntegerLiteralExpr\FalseExpr\TrueExpr in the printing phase directly*/
            if(!argType.equals("ast.IntegerLiteralExpr") && !argType.equals("ast.TrueExpr") && !argType.equals("ast.FalseExpr")){
                if(argType.equals("ast.ThisExpr")){
                    regNums.add("%this");
                }
                else{
                    arg.accept(this);
                    if (argType.equals("ast.NewObjectExpr")){
                        regNums.add("%_" + (regcnt-3));
                    }
                    else{
                        regNums.add("%_" + (regcnt-1));
                    } 
                }
            }
            else{
                regNums.add("unused");
            }
        }


        //Perform the call on the function pointer.the first argument is the receiver object ("this").
        String methodReturnType = getLLVMType(classVTM.EntryAtIndex(methodIndexinVTM).getRetType());
        appendWithIndent("%_" + regcnt + " = call " + methodReturnType + " " + methodPntrRegister + "(i8* " + ownerObjectRegister);
        regcnt++;

        //INBAL: NEED TO THINK ABOUT NEW OBJECT EXPR IN ACTUALS this.run(new A())
        //add (print) actuals to LLVM call line
        ArrayList<String> params = classVTM.EntryAtIndex(methodIndexinVTM).getParams();
        for (int i=0;i<numActuals;i++){
            builder.append(", ");
            builder.append(getLLVMType(params.get(i)));
            builder.append(" ");
            Expr arg = e.actuals().get(i);
            String argType = arg.getClass().getName();
            if(!argType.equals("ast.IntegerLiteralExpr") && !argType.equals("ast.TrueExpr") && !argType.equals("ast.FalseExpr")){
                builder.append(regNums.get(i));
            }
            else{
                arg.accept(this);
            }
        }
        builder.append(")");
        builder.append("\n"); 
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        builder.append(e.num());
    }

    @Override
    public void visit(TrueExpr e) {
        builder.append("1");
    }

    @Override
    public void visit(FalseExpr e) {
        builder.append("0");
    }

    @Override
    public void visit(IdentifierExpr e) {/*Loads identifier data to regcnt - 1*/
        String identifier = e.id();
        String regName = identifier;
        /*search for the identifier's decleration entry in symbol table.
        goes to it's Decl column to find out which type it is.*/
        String identifierClass = currST.getIdentifiersClass(identifier);
        classOfMethodCall = identifierClass;
        if(currST.isField(identifier)){/*if the identifier is a field, loads the field from the object's vtable */
            printFieldLoad(identifier);
            regName = "_" + (regcnt-1);
        }
        String obj_LLVM_type = getLLVMType(identifierClass);
        appendWithIndent("%_" + regcnt + " = load " + obj_LLVM_type + ", " + obj_LLVM_type + "* %" + regName);
        regcnt++;
        builder.append("\n");
    }

    public void visit(ThisExpr e) {

    }

    @Override
    public void visit(NewIntArrayExpr e) {
        long lentgh_reg=-1;
        if(e.lengthExpr().getClass().getName().equals("ast.IntegerLiteralExpr")){
            appendWithIndent("%_");
            builder.append(regcnt);
            regcnt++;
            builder.append(" = ");
            builder.append("icmp");
            builder.append(" slt i32 ");
            e.lengthExpr().accept(this);
            builder.append(", 0");
    }
    else{
        e.lengthExpr().accept(this);
        lentgh_reg=regcnt-1;
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("icmp");
        builder.append(" slt i32 ");
        builder.append("%_"+lentgh_reg);
        builder.append(", 0");
    }
        builder.append("\n");
        appendWithIndent("br i1 %_");
        builder.append(regcnt-1);
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append(", label %arr_alloc");
        builder.append(arrallocCount);
        arrallocCount++;
        builder.append("\n");
        builder.append("arr_alloc"); // new label
        builder.append(arrallocCount-2);
        builder.append(":\n");
        appendWithIndent("call void @throw_oob()") ;
        builder.append("\n");
        appendWithIndent("br label %arr_alloc");
        builder.append(arrallocCount-1);
        builder.append("\n");
        builder.append("arr_alloc"); //new label
        builder.append(arrallocCount-1);
        builder.append(":\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("add i32 ");
        if(e.lengthExpr().getClass().getName().equals("ast.IntegerLiteralExpr")){
                e.lengthExpr().accept(this);
            }
        else{
            builder.append("%_"+lentgh_reg);
        }
        builder.append(", 1");
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("call i8* @calloc(i32 4, i32 %_");
        builder.append(regcnt-2);
        builder.append(")");
        builder.append("\n");
        appendWithIndent("%_");
        builder.append(regcnt);
        regcnt++;
        builder.append(" = ");
        builder.append("bitcast i8* %_");
        builder.append(regcnt-2);
        builder.append(" to i32*");
        builder.append("\n");
        appendWithIndent("store i32 ");
        if(e.lengthExpr().getClass().getName().equals("ast.IntegerLiteralExpr")){
            e.lengthExpr().accept(this);
    }
    else{
        builder.append("%_"+lentgh_reg);
    }
        builder.append(", i32* %_");
        builder.append(regcnt-1);
        builder.append("\n");
    }

    /*Stores the new object in regcnt - 3*/
    @Override
    public void visit(NewObjectExpr e) {
        /*Allocate the required memory on heap for our object:
        first argument for calloc is amount of objects, second is object_size
        amount of objects is always 1 for object allocation.*/
        appendWithIndent("%_" + regcnt + " = call i8* @calloc(i32 1, i32 "); //INBAL: check if this is always i8*
        regcnt++;
        String object_class = e.classId();
        classOfMethodCall = object_class;
        VTFields class_VTFields = prog.getClassVTFields(object_class);
        int object_size = class_VTFields.getCurrIndex();//need to access classe's vtable and get it's size (of fields + pointer to VTM which is always 5)
        builder.append(object_size + ")");
        builder.append("\n");
        /*Set the vtable pointer to point to the correct vtable:*/
        /*bitcast the object pointer from i8* to i8***:*/
        appendWithIndent("%_" + regcnt + " = bitcast i8* %_" + (regcnt-1) + " to i8***");
        regcnt++;
        builder.append("\n");
        /*Get the address of the first element of the vtable:*/
        //%_2 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
        int numOfMethods=class_VTFields.getVT().getNumOfMethods();
        appendWithIndent("%_" + regcnt + " = getelementptr [" + numOfMethods + " x i8*], [" + numOfMethods + " x i8*]* ");
        builder.append("@." + object_class + "_vtable, i32 0, i32 0");
        regcnt++;
        builder.append("\n");
        /*Set the vtable to the correct address.*/
        appendWithIndent("store i8** %_" + (regcnt-1) + ", i8*** %_" + (regcnt-2));
        builder.append("\n");
    }

    @Override
    public void visit(NotExpr e) {
        String className;
        className=e.e().getClass().getName();
        if(className.equals("ast.FalseExpr") || className.equals("ast.TrueExpr")){
            appendWithIndent("%_" + Integer.toString((int)regcnt) + " = sub i1 1, ");
            e.e().accept(this);
            regcnt++;
            builder.append("\n");
        }
        else{
            e.e().accept(this);
            appendWithIndent("%_" + Integer.toString((int)regcnt) + " = sub i1 1, %_" + Integer.toString((int)regcnt - 1) +"\n");
            regcnt++;
        }
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
        tmpClass = t.id();
    }
}

