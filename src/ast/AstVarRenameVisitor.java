package ast;
import java.util.HashSet;
public class AstVarRenameVisitor implements Visitor{
    private SymbolTable currST;
    private String originalName;
    private int Line;
    private String newName;
    private String method_to_change_var_name="";
    private MethodDecl currMethod;
    private boolean change_field=false;
    private boolean field_is_hidden=false; /* true when we have a formal varibal or varibal decl that has the same name as the field we want to replace its name
    so we dont want to change any of his occurances in the function because it will relate to the formal/var decl and not to the field*/
    private String ancestor=""; /*class name of the greatest ancestor of the class in which our field decl exists*/
    private HashSet<String> ourTree = new HashSet<String>();/* holds the class names of all classes in which we will need to rename a field decl
    if it matches originalName */

    private boolean reachedAncestor = false;/*true when we are in a subtree of our ancestor, so we need to see if there are field assiments with
    originalName and rename them */

    public AstVarRenameVisitor(String originalName,int Line,String newName){
        this.Line = Line;
        this.originalName = originalName;
        this.newName = newName;
    }



    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        for (var fieldDecl : classDecl.fields()) {
               fieldDecl.type().accept(this);
               if(fieldDecl.name().equals(originalName)&&Line==fieldDecl.lineNumber){
                    fieldDecl.setName(newName);
                    change_field=true;
                    ancestor=classDecl.name(); /*the class name we declerd the field name we want to change*/
                    ourTree.add(ancestor);
                }
                fieldDecl.accept(this);
        }

        if(classDecl.name().equals(ancestor)){
            reachedAncestor = true;
        }
        else if(classDecl.superName() != null){
            if(ourTree.contains(classDecl.superName())){
                ourTree.add(classDecl.name());
                reachedAncestor = true;
            }
        }

        for(var mthd: classDecl.methoddecls()){
            currST = mthd.enclosingScope();
            mthd.accept(this);
        }
        reachedAncestor=false;
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }


    @Override
    public void visit(MethodDecl methodDecl) {
        currST = methodDecl.enclosingScope();
        currMethod=methodDecl;
        /*checks if we want to change field, if we do it checks in the
        ST of the method if there are entry with the same name of the field, 
        if we do have it is hiding the field so field is hiden became true*/
        if(change_field && currST.checkEntryExists(originalName)){ 
            field_is_hidden=true;
        }
        /*if we in the subtree of the class we decl the field and there isnt a var in the ST of the method
        with the same name we set the method to change occurances of the name of the field we wnat to change to the curr method*/
        if(reachedAncestor && !(field_is_hidden)){
            method_to_change_var_name=methodDecl.name();
        }
         for (var formal : methodDecl.formals()) { 
            if(formal.name().equals(originalName)&&Line==formal.lineNumber){
                method_to_change_var_name=methodDecl.name(); /*set the method name we want to change occurance of the name of the variable */
                formal.setName(newName);
            }
            formal.accept(this);
         }

        for (var varDecl : methodDecl.vardecls()) {
                if(varDecl.name().equals(originalName)&&Line==varDecl.lineNumber){
                method_to_change_var_name=methodDecl.name(); /*set the method name we want to change occurance of the name of the variable */
                varDecl.setName(newName);
            }
            varDecl.accept(this);
        }

        /*for every statement in method body*/
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        /*for the return line*/
        methodDecl.ret().accept(this);
        field_is_hidden=false; /* we finished with this method so field is hiden is false again*/
        method_to_change_var_name="";

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
        if(currMethod.name().equals(method_to_change_var_name) && assignStatement.lv().equals(originalName)){
                assignStatement.setLv(newName);
                }
        

        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        if(currMethod.name().equals(method_to_change_var_name) && assignArrayStatement.lv().equals(originalName)){
                assignArrayStatement.setLv(newName);
                }
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
        e.ownerExpr().accept(this);
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
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
    public void visit(ThisExpr e) {
        return;

    }

    @Override
    public void visit(IntAstType t) {
        return;

    }

    @Override
    public void visit(BoolAstType t) {
        return;

    }

    @Override
    public void visit(IntArrayAstType t) {
        return;

    }

    @Override
    public void visit(RefType t) {
        return;

    }




    @Override
	public void visit(VarDecl varDecl) {
		return;
		
    }
    
    @Override
	public void visit(IdentifierExpr e) {
        if(currMethod.name().equals(method_to_change_var_name) && e.id().equals(originalName)){
            e.setId(newName);
        }
		return;
		
	}

    @Override
	public void visit(FalseExpr e) {
		return;
		
    }
    @Override
	public void visit(TrueExpr e) {
		return;
		
    }
    @Override
	public void visit(FormalArg e) {
		return;
		
    }
    @Override
	public void visit(IntegerLiteralExpr e) {
		return;
		
    }
    
    

}