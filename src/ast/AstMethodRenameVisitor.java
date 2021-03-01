package ast;
import java.util.HashSet;

public class AstMethodRenameVisitor implements Visitor {

    private SymbolTable ourMethodST; /*the ST which cotains the method decleration of the method we are renaming*/
    private SymbolTable ancestorST; /*the ST of the class that is the greatest ancestor of the class in which our method decl exists,
    which has a method decl with the same name*/
    private boolean isOwnerExpression = false;/*value is true only when we are in an astNode which is in a subtree of an ownerExpression*/
    private HashSet<String> ourTree = new HashSet<String>();/*holds the class names of all classes in which we will need to rename a method decl
    if it matches originalName */
    private boolean reachedAncestor = false;/*true when we are in a subtree of our ancestor, so we need to see if there are calls\method decl with
    originalName and rename them */
    private SymbolTable currST; /*points to the ST of the enclosing scope of our current location in the program*/
    private Program prog;

    private String originalName;
    private int Line;
    private String newName;

    public AstMethodRenameVisitor(String originalName,int Line,String newName){
        this.Line = Line;
        this.originalName = originalName;
        this.newName = newName;
    }

    @Override
    public void visit(Program program) {
        prog = program;
        /*find ourmethodST */
        setOurMethodST(prog);
        findAncestor(ourMethodST);
        ourTree.add(ancestorST.getSTclassName());
        /*add classes that interest us to ourTree*/
        for (ClassDecl classDecl : program.classDecls()) {
            if(classDecl.superName() != null){
                if(ourTree.contains(classDecl.superName())){
                    ourTree.add(classDecl.name());
                }
            }
        }
        /*do the renaming*/
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
        program.mainClass().accept(this);
    }

    @Override
    public void visit(ClassDecl classDecl) {
        currST = classDecl.enclosingScope();

        /*check if we are currently in the sub tree in which we need to rename all method decls if method name is originalName */
        if (ourTree.contains(classDecl.name())){
            reachedAncestor = true;
        }

        for(var mthd: classDecl.methoddecls()){
            mthd.accept(this);
        }
        currST = classDecl.enclosingScope();
        reachedAncestor = false;
        
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    /*goes over all method declerations and finds the declaration of the method that we are renaming.
    Sets the ourMethodST to it's ST and the ancestor to it's class name.*/
    public void setOurMethodST(Program program){
        for (ClassDecl classdecl : program.classDecls()) {
            for (MethodDecl methodDecl : classdecl.methoddecls()){
                if(methodDecl.name().equals(originalName) && methodDecl.lineNumber == Line){
                    ourMethodST = methodDecl.enclosingScope().getParent();
                    methodDecl.setName(newName);
                    return;
                }
            }
        }
    }



    /*sets the ancestor and ancestorST fields to the class that is the greatest ancestor of the class in which our method decl exists,
    which has a method decl with the same name.
    All of the method decl that exist in the subtree of the ancestor interest us because if they match originalName we must rename them*/
    public void findAncestor(SymbolTable ourMethodST){
        ancestorST = ourMethodST;
        // ancestor
        SymbolTable prnt = ourMethodST.getParent();
        while (prnt != null){
            if (prnt.checkMethodDeclEntryExists(originalName)){
                ancestorST = prnt;
            }
            prnt = prnt.getParent();
        }
    }
  

    @Override
    public void visit(MethodDecl methodDecl) {
        currST = methodDecl.enclosingScope();
        if (methodDecl.name().equals(originalName) && reachedAncestor){
            methodDecl.setName(newName);
        }
        for (var stmt : methodDecl.body()) {  
            stmt.accept(this); 
        }
        methodDecl.ret().accept(this);
    }


    @Override
    public void visit(BlockStatement blockStatement) {
        for (var stmt : blockStatement.statements()) {
            stmt.accept(this);
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
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(LtExpr e) {
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(AddExpr e) {
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(SubtractExpr e) {
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(MultExpr e) {
        e.e1().accept(this);
        e.e2().accept(this);
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
        
        String methodName = e.methodId();
        if(methodName.equals(originalName)){
            boolean prevReachedAncestor = reachedAncestor;
            isOwnerExpression = true;
            e.ownerExpr().accept(this);
            isOwnerExpression = false;
            if (reachedAncestor){
                e.setMethodId(newName);
            }
            reachedAncestor = prevReachedAncestor;           
        }
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    /*sets currST to the ST of new object's class*/
    @Override
    public void visit(NewObjectExpr e) {
        String className = e.classId();
        if (isOwnerExpression){
            if (ourTree.contains(className)){
                reachedAncestor = true;
            }
            else{
                reachedAncestor = false;
            }
        }
    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(ThisExpr e) {
        /*doesn't have any fields other than line number*/
        return;
    }

    @Override
    public void visit(IntAstType t) {
        /*doesn't have any fields other than line number*/
        return;

    }

    @Override
    public void visit(BoolAstType t) {
        /*doesn't have any fields other than line number*/
        return;

    }

    @Override
    public void visit(IntArrayAstType t) {
        /*doesn't have any fields other than line number*/
        return;

    }

    @Override
    public void visit(RefType t) {
        /*two fields: id and line number*/
        return;

    }


    @Override
	public void visit(VarDecl varDecl) {
		return;
		
    }
    
    @Override
	public void visit(IdentifierExpr e) {
        String identifier = e.id();

        /*search for the identifier's decleration entry in symbol table.
        goes to it's Decl column to find out which class it belongs to.*/
        String identifierClass = currST.getIdentifiersClass(identifier);

        if (isOwnerExpression){
            if (ourTree.contains(identifierClass)){
                reachedAncestor = true;
            }
            else{
                reachedAncestor = false;
            }
        }

	}

    @Override
	public void visit(FalseExpr e) {
        /*doesn't have any fields other than line number*/
		return;
		
    }
    @Override
	public void visit(TrueExpr e) {
        /*doesn't have any fields other than line number*/
		return;
		
    }
    @Override
	public void visit(FormalArg e) {
        /*INBAL: i don't think this interests us because it's the parameters in the method's decl
        and we handled checking a var\field\formal's type in the ST.getIdentifierClass function*/
		return;
		
    }
    @Override
	public void visit(IntegerLiteralExpr e) {
        /*two fields: num and line number */
		return;
	}
}