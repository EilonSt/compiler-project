package ast;

public class AstSymbolTableVisitor implements Visitor{
    private Program prog;
    private String tmpClass;
    
    @Override
    public void visit(Program program) {
        prog = program;
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {  
            program.addClass(classdecl.name(), classdecl.enclosingScope());
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        classDecl.enclosingScope().setSTclassName(classDecl.name());
        
        if (classDecl.superName() != null) {
            SymbolTable prnt = prog.getClassST(classDecl.superName());
            classDecl.enclosingScope().setParent(prnt);
        }
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.type().accept(this);
            if (fieldDecl.lineNumber != null){
                classDecl.enclosingScope().addEntry(fieldDecl.name(), "field", tmpClass, fieldDecl.lineNumber);
            }
            else{
                classDecl.enclosingScope().addEntry(fieldDecl.name(), "field", tmpClass, 0);
            }
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            String decl = "";
            for (var formal : methodDecl.formals()){
                formal.type().accept(this);
                decl = decl + tmpClass + " ";
            }
            methodDecl.returnType().accept(this);
            decl = decl + tmpClass;
            if (methodDecl.lineNumber != null){
                classDecl.enclosingScope().addEntry(methodDecl.name(),"method",decl,methodDecl.lineNumber);
            }
            else{
                classDecl.enclosingScope().addEntry(methodDecl.name(),"method",decl,0);
            }
            methodDecl.enclosingScope().setParent(classDecl.enclosingScope());
            methodDecl.accept(this);
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
            /*Eilon need to add to symboltable of the method fields*/
            formal.type().accept(this);
            if (formal.lineNumber != null){
                methodDecl.enclosingScope().addEntry(formal.name(), "formal", tmpClass , formal.lineNumber);
            }
            else{
                methodDecl.enclosingScope().addEntry(formal.name(), "formal", tmpClass , 0);
            }
            //Inbal changed this - formal doesn't need an AST node formal.enclosingScope().setParent(methodDecl.enclosingScope());
            formal.accept(this);
            
        }

        for (var varDecl : methodDecl.vardecls()) {
            /*Eilon need to add to symboltable of the method vars */
            varDecl.type().accept(this);
            if(varDecl.lineNumber != null){
                methodDecl.enclosingScope().addEntry(varDecl.name(), "var", tmpClass , varDecl.lineNumber);
            }
            else{
                methodDecl.enclosingScope().addEntry(varDecl.name(), "var", tmpClass , 0);
            }
            // inbal changed this varDecl.enclosingScope().setParent(methodDecl.enclosingScope());
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
            /*Eilon need to add to symboltable of the block vars */
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
