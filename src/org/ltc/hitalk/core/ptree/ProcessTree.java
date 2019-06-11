package org.ltc.hitalk.core.ptree;

/**
 *
 */
public
class ProcessTree implements IRewriter <IRewrittable> {
//
//    private IRewriteRule<IRewrittable> pipeline;
//    private DriveContext initialContext;
//
//    public
//    void init () {
////        pipeline.push(GraphRewriteSteps.CompleteCurrentNodeStep.new IRewriteRule.RewriteRule.T1(this, ProcessTree::preconditionT1, ProcessTree::rewriteT1, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.T2(this, ProcessTree::preconditionT2, ProcessTree::rewriteT2, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.B1(this, ProcessTree::preconditionB1, ProcessTree::rewriteB1, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.B2(this, ProcessTree::preconditionB2, ProcessTree::rewriteB2, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.B3(this, ProcessTree::preconditionB3, ProcessTree::rewriteB3, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.B4(this, ProcessTree::preconditionB4, ProcessTree::rewriteB4, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.C1(this, ProcessTree::preconditionC1, ProcessTree::rewriteC1, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.C2(this, ProcessTree::preconditionC2, ProcessTree::rewriteC2, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.D1(this, ProcessTree::preconditionD1, ProcessTree::rewriteD1, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.D2(this, ProcessTree::preconditionD2, ProcessTree::rewriteD2, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.D3(this, ProcessTree::preconditionD3, ProcessTree::rewriteD3, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.D4(this, ProcessTree::preconditionD4, ProcessTree::rewriteD4, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.O1(this, ProcessTree::preconditionO1, ProcessTree::rewriteO1, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.O2(this, ProcessTree::preconditionO2, ProcessTree::rewriteO2, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.R1(this, ProcessTree::preconditionR1, ProcessTree::rewriteR1, 1));
//        pipeline.push(new IRewriteRule.RewriteRule.R2(this, ProcessTree::preconditionR2, ProcessTree::rewriteR2, 1));
//    }

    private Node root;
//    private List <ProcessTreeListener> listeners= new ArrayList <>(1);

    public
    ProcessTree ( Node root ) {
        this.root = root;
    }

    public
    ProcessTree () {

    }

//    public
//    void addRoot ( ICallable rootTerm, IThreadModel tm, DriveContext dc ){
//        rootTerm = new Configuration(System.current)
//        try {
//            this.root = new Node((Configuration) rootTerm, create(dm));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public
    Node getRoot () {
        return root;
    }

//    public
//    Node replaceNode ( Node node, Term term ) {
//        return null;
//    }

    public
    boolean isRoot ( Node node ) {
        return node == root;
    }


    public
    void setNodeState ( Node node, Node.NodeState processed ) {
        node.setState(processed);
    }

    public
    Node addThreadNode ( CallableTerm term, DriveContext dc ) {
        return null;
    }

    //    public
//    void addListener ( ProcessTreeListener listener) {
//        listeners.add(listener);
//
//    }
//
//    public
    void fold ( Node alpha, Node beta ) {


    }
//
//    public
//    boolean unify ( IRewrittable rewrittable ) {
//        return false;
//    }
//
//    /**
//     * @return
//     */
//    @Override
//    public
//    IRewriteRule <IRewrittable> getPipeline () {
//        return null;
//    }
//
//    public
//    boolean addConstraint ( IRewrittable constraint ) {
//        return false;
//    }
//
//    public
//    void setTo ( boolean b ) {
//
//    }
//
//    public
//    Boolean preconditionT1 ( IRewriter <IRewrittable> rewriter, IRewrittable rewritter ) {
//        return null;
//    }
//
//    public
//    DriveContext getInitialContext () {
//        return initialContext;
//    }
//
//    public
//    void setProperty ( Node currentNode, Node.NodeState state, Node.NodeState state1 ) {
//
//    }
//
//    public static
//    class Node implements IPtElement {
//
//        private Configuration configuration;
//        //   private DriveContext context;
//        private NodeState nodeState= NodeState.INACTIVE;
//
//        private CallableTerm result;
//
//        public
//        Node ( Configuration configuration, DriveContext context ) {
//            this.configuration = configuration;
//
//
//            //        this.context = context;
//
//            //        this.nodeState = nodeState;
//        }
//
//        public
//        Node ( Configuration configuration, NodeState nodeState ) {
//            this.configuration = configuration;
//            this.nodeState = nodeState;
//        }
//
//        public
//        Configuration getConfiguration () {
//            return configuration;
//        }
//
//        public
//        Node getSuccessor ( int i ) {
//            return getConfiguration().getOuts().get(i).getChildNode();
//        }
//
//        public
//        Node getPredecessor () {
//            return getConfiguration().getIn().getParentNode();
//        }
//
//        public
//        NodeState getNodeState () {
//            return nodeState;
//        }
//
//        public
//        CallableTerm getResult () {
//            return result;
//        }
//
//        public
//        NodeState getState () {
//            return nodeState;
//        }
//
//        public
//        void setState ( NodeState state ) {
//            nodeState = state;
//        }
//
//        public
//        CallableTerm getTerm () {
//            return result;
//        }
//
//        public
//        DriveContext getContext ( ProcessTree processTree ) {
//            Node root = processTree.getRoot();
//            ProcessTreePath path = ProcessTreePath.getPTreePath(root, this);
//            return path.getContext(processTree);
//        }
//
//        public
//        void setResidualValue ( CallableTerm result ) {
//            this.result = result;
//        }
//
//        public
//        boolean hasSuccessors () {
//            return getConfiguration().getOuts().size() > 0;
//        }
//
//        public
//        Node addSuccessor ( Node n ) {
//            ////    successors.add(n);
//            //        successors.get(successors.size() - 1).predecessors.add( n);
//            return n;
//        }
//
//        //    public
//        //    void addSibling ( Node n ) {
//        //
//        //
//        //    }
//        //
//        //    @Override
//        //    public
//        //    boolean pUnify ( IRewrittable rewrittable ) {
//        //        if(unify(rewrittable)){
//        //            return true;
//        //        }
//        //        return disUnify(rewrittable);
//        //    }
//
//        public
//        boolean disUnify ( IRewrittable rewrittable ) {
//            return false;
//        }
//
//
//        /**
//         * @param rewrittable
//         * @return
//         */
//        @Override
//        public
//        boolean unify ( IRewrittable rewrittable ) {
//            return false;
//        }
//
//        public
//        int indexOf () {
//            return getPredecessor().getConfiguration().getOuts().indexOf(configuration.getIn());
//        }
//
//        private
//        int getNumSuccessors () {
//            return getConfiguration().getOuts().size();
//        }

    public
    enum NodeState {
        INACTIVE,
        ACTIVE,
        COMPLETE,
        PROCESSED,
        ABRUPTED,
        REMOVED,
        STATE,
    }

}


