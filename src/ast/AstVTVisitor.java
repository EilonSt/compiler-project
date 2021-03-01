package ast;

public class AstVTVisitor implements Visitor{
    private Program prog;
    private String tmpClass;/*set during type() visits.set to "int"\"int-array"\"bool"\class_name according to the type of the object.*/
    
    @Override
    public void visit(Program program) {
        prog = program;
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {  
            program.addClassVTFields(classdecl.name(), classdecl.VTF);
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        
        if (classDecl.superName() != null) {
            classDecl.VTF.copyVTF(prog.getClassVTFields(classDecl.superName()));
        }
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.type().accept(this);
            int byte_size = 0;
            String type  = null;
            switch (tmpClass){
                case "int":
                    byte_size = 4;
                    break;
                case "bool":
                    byte_size = 1;
                    break;
                case "int-array":
                    byte_size = 8;
                    break;
                default:
                    byte_size = 8;
                    tmpClass = "ref";
            }
            type = tmpClass;
            classDecl.VTF.addField(fieldDecl.name(), type, byte_size);
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            boolean isOverride = false;
            int not_exist = -1;
            //VTFields f = classDecl.VTF;
            VTMethods VTM = classDecl.VTF.getVT();
            String mn = methodDecl.name();
            int exist = VTM.methodExist(mn);
            /*Override happend*/
            if(exist != not_exist){
                isOverride = true;
                classDecl.VTF.getVT().EntryAtIndex(exist).setClassName(classDecl.name());
            }
            else{
                methodDecl.returnType().accept(this);
                exist = classDecl.VTF.getVT().addMethodDecl(methodDecl.name(),classDecl.name(), tmpClass);
            }
            /*go over formals to add their types to the ST decl field in the methodDecl entry.
            I think we don't actually use this data anywhere later*/
            if(!isOverride){
                for (var formal : methodDecl.formals()){
                    formal.type().accept(this);
                    classDecl.VTF.getVT().EntryAtIndex(exist).addParam(tmpClass);
                }
            }
            
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        prog.addClass(mainClass.name(), mainClass.enclosingScope());
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        methodDecl.returnType().accept(this);

        for (var formal : methodDecl.formals()) {
            formal.type().accept(this);
            methodDecl.enclosingScope().addEntry(formal.name(), "formal", tmpClass , formal.lineNumber);
            formal.accept(this);    
        }

        for (var varDecl : methodDecl.vardecls()) {
            varDecl.type().accept(this);
            methodDecl.enclosingScope().addEntry(varDecl.name(), "var", tmpClass , varDecl.lineNumber);
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.enclosingScope().setParent(methodDecl.enclosingScope());
            stmt.accept(this);
        }
 
        methodDecl.ret().accept(this);
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

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ifStatement.elsecase().accept(this);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
    }

    @Override
    public void visit(AndExpr e) {
        return;
    }

    @Override
    public void visit(LtExpr e) {
        return;
    }

    @Override
    public void visit(AddExpr e) {
        return;
    }

    @Override
    public void visit(SubtractExpr e) {
        return;
    }

    @Override
    public void visit(MultExpr e) {
        return;
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        e.indexExpr().accept(this);
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);
    }

    @Override
    public void visit(MethodCallExpr e) {
        e.ownerExpr().accept(this);
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        return;
    }

    @Override
    public void visit(TrueExpr e) {
        return;
    }

    @Override
    public void visit(FalseExpr e) {
        return;
    }

    @Override
    public void visit(IdentifierExpr e) {
        return;
    }

    public void visit(ThisExpr e) {
        return;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {
        return;
    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
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