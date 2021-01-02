package com.android.server.pm.sepolicy;

import android.util.Slog;

import com.android.server.pm.sepolicy.PolicyLexer.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PolicyParser {

    private static final String TAG = "PolicyParser";
    private static final boolean DEBUG_SEPOLICY = false;

    private static final String UNTRUSTED_APP = "untrusted_app";
    private static final String ISOLATED_APP = "isolated_app";

    private static final String APP_DATA_FILE = "app_data_file";
    private static final String APPDOMAIN_TMPFS = "appdomain_tmpfs";

    private static final String CIL_KEY_BLOCK = "block";
    private static final String CIL_KEY_SELF = "self";
    private static final String CIL_KEY_TYPE = "type";
    private static final String CIL_KEY_TYPEBOUNDS  = "typebounds";
    private static final String CIL_KEY_TYPEATTRIBUTE  = "typeattribute";
    private static final String CIL_KEY_TYPEATTRIBUTESET  = "typeattributeset"; // doesn't support expr
    private static final String CIL_KEY_TYPETRANSITION  = "typetransition";

    private static final ArrayList<String> CIL_KEY_AVRULES = new ArrayList<>(
            Arrays.asList("allow", "auditallow", "dontaudit", "neverallow", "allowx", "auditallowx",
                    "dontauditx","neverallowx")
    );

    private static final ArrayList<String> CIL_KEY_UNSUPPORTED = new ArrayList<>(
            Arrays.asList(
                    "*", "tunableif", "typechange", "call", "tunable", "role", "user",
                    "userattribute", "userattributeset", "sensitivity", "category", "categoryset",
                    "level", "levelrange", "class", "ipaddr", "classmap", "classpermission",
                    "boolean", "string", "name", "handleunknown", "blockinherit", "blockabstract",
                    "classorder", "classmapping", "classpermissionset", "common", "classcommon",
                    "sid", "sidcontext", "sidorder", "userlevel", "userrange", "userbounds",
                    "userprefix", "selinuxuser", "selinuxuserdefault", "typealias",
                    "typealiasactual", "typepermissive", "rangetransition", "userrole", "roletype",
                    "roletransition", "roleallow", "roleattribute", "roleattributeset",
                    "rolebounds", "booleanif", "typemember", "sensitivityalias",
                    "sensitivityaliasactual", "categoryalias", "categoryaliasactual",
                    "categoryorder", "sensitivityorder", "sensitivitycategory", "constrain",
                    "mlsconstrain", "validatetrans", "mlsvalidatetrans", "context", "filecon",
                    "ibpkeycon", "ibendportcon", "portcon", "nodecon", "genfscon", "netifcon",
                    "pirqcon", "iomemcon", "ioportcon", "pcidevicecon", "devicetreecon", "fsuse",
                    "policycap", "optional", "defaultuser", "defaultrole", "defaulttype", "macro",
                    "in", "mls", "defaultrange", "permissionx")
    );

    public static class ParseTree {

        private NonTerminal root, current;

        private String namespace;
        private ArrayList<String> types;
        private HashMap<String, ArrayList<String>> typebounds;
        private ArrayList<String> typetransition;
        private ArrayList<String> typeattribute;
        private HashMap<String, ArrayList<String>> typeattributeset;
        private HashSet<String> avrule_src;
        private HashSet<String> avrule_tgt;

        public ParseTree() {
            root = new NonTerminal(null);
            current = root;

            namespace = null;
            types = new ArrayList<>();
            typebounds = new HashMap<>();
            typetransition = new ArrayList<>();
            typeattribute = new ArrayList<>();
            typeattributeset = new HashMap<>();
            avrule_src = new HashSet<>();   // to avoid slowing down on duplicates
            avrule_tgt = new HashSet<>();   // to avoid slowing down on duplicates
        }

        private void addNonTerminal() {
            NonTerminal node = new NonTerminal(current);
            if (current == null)
                root = node;
            else
                current.children.add(node);
            current = node;
        }

        private void closeNonTerminal() {
            current = current.parent;
        }

        private void addTerminal(String data){
            Node node = new Terminal(current, data);
            current.children.add(node);
        }

        public boolean isCompliant(String pkgName) {

            if(!blockWrapped())
                return false;

            if(!treeWalk(root))
                return false;

            if (DEBUG_SEPOLICY) {
                Slog.d(TAG, "POLICY WRAPPED INTO BLOCK WITH NAMESPACE: " + namespace);
                Slog.d(TAG, "TYPES DECLARED");
                for (String type : types)
                    Slog.d(TAG, type);
                Slog.d(TAG, "TYPEBOUNDS DECLARED");
                for (String parent : typebounds.keySet()) {
                    StringBuilder sb = new StringBuilder(parent);
                    sb.append(": ");
                    ArrayList<String> child = typebounds.get(parent);
                    for (String type : child) {
                        sb.append(type);
                        sb.append(", ");
                    }
                    sb.delete(sb.lastIndexOf(","), sb.length());
                    Slog.d(TAG, sb.toString());
                }
                Slog.d(TAG, "TYPETRANSITION DECLARED");
                for (String dest : typetransition)
                    Slog.d(TAG, dest);
                Slog.d(TAG, "TYPEATTRIBUTE DECLARED");
                for (String typeattribute : typeattribute)
                    Slog.d(TAG, typeattribute);
                Slog.d(TAG, "TYPES USED IN TYPEATTRIBUTESET");
                for (String typeattribute : typeattributeset.keySet()) {
                    StringBuilder sb = new StringBuilder(typeattribute);
                    sb.append(": ");
                    ArrayList<String> set = typeattributeset.get(typeattribute);
                    for (String type : set) {
                        sb.append(type);
                        sb.append(", ");
                    }
                    sb.delete(sb.lastIndexOf(","), sb.length());
                    Slog.d(TAG, sb.toString());
                }
                Slog.d(TAG, "TYPES USED IN ALLOW STMT AS SRC");
                for (String type : avrule_src)
                    Slog.d(TAG, type);

                Slog.d(TAG, "TYPES USED IN ALLOW STMT AS TGT");
                for (String type : avrule_tgt)
                    Slog.d(TAG, type);
            }

            if (!namespace.equals(pkgName.replace(".","_")))
                return false;

            for (String type : types)
                if (type.equals(UNTRUSTED_APP) || type.equals(APP_DATA_FILE)
                        || typeattribute.contains(type))
                    return false;

            // not required as typeattribute cannot take part in a typebounds stmt
            for (String type : typeattribute)
                if (type.equals(UNTRUSTED_APP) || type.equals(APP_DATA_FILE))
                    return false;

            // TODO: resolve typeattributeset once and for all, avoid to hide externally
            //       defined typeattribute

            // resolve type bounded to untrusted_app/app_data_file
            ArrayList<String> queue = new ArrayList<>(Arrays.asList(UNTRUSTED_APP, APP_DATA_FILE));
            ArrayList<String> bounded_to_untrusted_app = new ArrayList<>();
            while (!queue.isEmpty()) {
                String vertex = queue.get(0);
                ArrayList<String> neighbors = typebounds.get(vertex);
                if (neighbors != null) {
                    for (String neighbor : neighbors) {
                        if (!bounded_to_untrusted_app.contains(neighbor)) {
                            bounded_to_untrusted_app.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                queue.remove(0);
            }

            if (DEBUG_SEPOLICY) {
                Slog.d(TAG, "TYPES BOUNDED");
                for (String type : bounded_to_untrusted_app)
                    Slog.d(TAG, type);
            }

            for (String typeattr : typeattributeset.keySet()) {
                // all elements in the set of the global typeattribute must be local and bounded
                if (!typeattribute.contains(typeattr)) {
                    ArrayList<String> list = typeattributeset.get(typeattr);
                    for (String type : list) {
                        if (!(types.contains(type) && bounded_to_untrusted_app.contains(type)
                                || typeattribute.contains(type) && resolveTypeAttribute(type) != null
                                        && bounded_to_untrusted_app.containsAll(resolveTypeAttribute(type))))
                            return false;
                    }
                }
            }

            for (String dest : typetransition) {
                // all typetransition must be local and bounded with the exception of appdomain_tmpfs
                if (!(types.contains(dest) && bounded_to_untrusted_app.contains(dest)
                        || typeattribute.contains(dest) && resolveTypeAttribute(dest) != null
                            && bounded_to_untrusted_app.containsAll(resolveTypeAttribute(dest))
                        || dest.equals(APPDOMAIN_TMPFS)))
                    return false;
            }

            for (String src : avrule_src) {
                // all avrule_src must be local and bounded
                if (!(types.contains(src) && bounded_to_untrusted_app.contains(src)
                        || typeattribute.contains(src) && resolveTypeAttribute(src) != null
                                && bounded_to_untrusted_app.containsAll(resolveTypeAttribute(src))))
                    return false;
            }

            for (String tgt : avrule_tgt) {
                // all avrule_tgt must not be defined by another package
                if (tgt.contains(".") && !tgt.startsWith(".") && !validGlobalType(tgt))
                    return false;
            }
            return true;
        }

        private boolean blockWrapped() {
            if (root == null || root.children.size() != 1 || root.children.get(0) instanceof Terminal)
                return false;
            NonTerminal nt = (NonTerminal) root.children.get(0);

            if (nt.children.size() < 2  || nt.children.get(0) instanceof NonTerminal || nt.children.get(1) instanceof NonTerminal)
                return false;
            Terminal key = (Terminal) nt.children.get(0), namespace = (Terminal) nt.children.get(1);

            if (!key.data.equals(CIL_KEY_BLOCK))
                return false;

            this.namespace = namespace.data;
            return true;
        }

        private boolean treeWalk(Node node) {
            if (node == null || node instanceof Terminal)
                return true;
            NonTerminal nt = (NonTerminal) node;
            for (Node n : nt.children) {
                if (!processNode(n) || !treeWalk(n))
                    return false;
            }
            return true;
        }

        private boolean processNode(Node node) {

            if (node instanceof Terminal)
                return true;
            NonTerminal nt = (NonTerminal) node;

            if (nt.children.isEmpty() || nt.children.get(0) instanceof NonTerminal)
                return false;
            Terminal key = (Terminal) nt.children.get(0);

            if (key.data.equals(CIL_KEY_TYPE)) {

                if (nt.children.size() != 2 || nt.children.get(1) instanceof NonTerminal)
                    return false;
                Terminal type = (Terminal) nt.children.get(1);
                types.add(type.data);

            } else if (key.data.equals(CIL_KEY_TYPEBOUNDS)) {

                if (nt.children.size() != 3 || nt.children.get(1) instanceof NonTerminal || nt.children.get(2) instanceof NonTerminal)
                    return false;
                Terminal parent = (Terminal) nt.children.get(1);
                Terminal child = (Terminal) nt.children.get(2);
                if (!typebounds.containsKey(parent.data)) {
                    typebounds.put(parent.data, new ArrayList<>(Collections.singletonList(child.data)));
                } else {
                    ArrayList<String> list = typebounds.get(parent.data);
                    list.add(child.data);
                }

            } else if (key.data.equals(CIL_KEY_TYPETRANSITION)) {

                if (nt.children.size() < 5 || nt.children.size() > 6
                        || nt.children.get(1) instanceof NonTerminal
                        || nt.children.get(2) instanceof NonTerminal
                        || nt.children.get(3) instanceof NonTerminal
                        || nt.children.get(4) instanceof NonTerminal
                        || nt.children.get(nt.children.size() - 1) instanceof NonTerminal)
                    return false;
                Terminal dest = (Terminal) nt.children.get(nt.children.size() - 1);
                typetransition.add(dest.data);

            } else if (key.data.equals(CIL_KEY_TYPEATTRIBUTE)) {

                if (nt.children.size() != 2 || nt.children.get(1) instanceof NonTerminal)
                    return false;
                Terminal typeattr = (Terminal) nt.children.get(1);
                typeattribute.add(typeattr.data);

            } else if (key.data.equals(CIL_KEY_TYPEATTRIBUTESET)) {

                if (nt.children.size() != 3 || nt.children.get(1) instanceof NonTerminal)
                    return false;
                Terminal typeattribute = (Terminal) nt.children.get(1);
                if (nt.children.get(2) instanceof Terminal) {
                    Terminal set = (Terminal) nt.children.get(2);
                    if (!typeattributeset.containsKey(typeattribute.data)) {
                        typeattributeset.put(typeattribute.data, new ArrayList<>(Collections.singletonList(set.data)));
                    } else {
                        ArrayList<String> list = typeattributeset.get(typeattribute.data);
                        list.add(set.data);
                    }
                } else if (nt.children.get(2) instanceof NonTerminal) {
                    NonTerminal set = (NonTerminal) nt.children.get(2);
                    ArrayList<String> list = new ArrayList<>();
                    for (int i = 0; i < set.children.size(); i++) {
                        if (set.children.get(i) instanceof NonTerminal)
                            return false;
                        Terminal t = (Terminal) set.children.get(i);
                        list.add(t.data);
                    }
                    if (!typeattributeset.containsKey(typeattribute.data)) {
                        typeattributeset.put(typeattribute.data, list);
                    } else {
                        ArrayList<String> prev_list = typeattributeset.get(typeattribute.data);
                        prev_list.addAll(list);
                    }
                }

            } else if (CIL_KEY_AVRULES.contains(key.data)) {

                if (nt.children.size() != 4 || nt.children.get(1) instanceof NonTerminal || nt.children.get(2) instanceof NonTerminal)
                    return false;
                Terminal src = (Terminal) nt.children.get(1);
                Terminal tgt = (Terminal) nt.children.get(2);
                avrule_src.add(src.data);
                if (!CIL_KEY_SELF.equals(tgt.data))
                    avrule_tgt.add(tgt.data);
                else
                    avrule_tgt.add(src.data);
            } else if (CIL_KEY_UNSUPPORTED.contains(key.data)) {
                return false;
            }

            return true;
        }

        private ArrayList<String> resolveTypeAttribute(String typeattr) {
            if (!typeattributeset.containsKey(typeattr))
                return new ArrayList<>();

            ArrayList<String> solved = new ArrayList<>();
            ArrayList<String> ret = new ArrayList<>(typeattributeset.get(typeattr));
            int i = 0;
            while (i < ret.size()) {
                String toSolve = ret.get(i);
                if (solved.contains(toSolve))
                    return null;
                if (typeattributeset.containsKey(toSolve)) {
                    solved.add(toSolve);
                    ret.addAll(typeattributeset.get(toSolve));
                    ret.remove(i);
                } else {
                    i++;
                }
            }
            return ret;
        }

        private String getLocalType(String type) {
            if (type != null && type.startsWith(namespace + "."))
                return type.substring(namespace.length() + 1);
            return null;
        }

        private boolean validGlobalType(String type) {
            String localType = getLocalType(type);
            return (localType != null && types.contains(localType));
        }

        public boolean validTypes(ArrayList<String> fcTypes) {
            for (String type : fcTypes)
                if (!validGlobalType(type) && !APP_DATA_FILE.equals(type))
                    return false;
            return true;
        }

        public boolean validDomains(ArrayList<String> seappDomains) {
            for (String domain : seappDomains)
                if (!validGlobalType(domain) && !UNTRUSTED_APP.equals(domain)
                        && !ISOLATED_APP.equals(domain))
                    return false;
            return true;
        }

        private class Node {

            protected NonTerminal parent;

            private Node(NonTerminal parent) {
                this.parent = parent;
            }
        }

        private class NonTerminal extends Node {

            private List<Node> children;

            private NonTerminal(NonTerminal parent) {
                super(parent);
                children = new ArrayList<>();
            }
        }

        private class Terminal extends Node {
            private String data;

            private Terminal(NonTerminal parent, String data) {
                super(parent);
                this.data = data;
            }
        }

    }

    public static ParseTree parse(List<Token> tokens) {

        int paren_count = 0;
        ParseTree parseTree = new ParseTree();

        for (Token tok : tokens) {

            switch (tok.type) {
                case OPAREN:
                    paren_count++;
                    parseTree.addNonTerminal();
                    break;
                case CPAREN:
                    paren_count--;
                    if (paren_count < 0)
                        throw new RuntimeException(String.format("Close parenthesis without matching open at line %d\n", tok.line));
                    parseTree.closeNonTerminal();
                    break;
                case QSTRING:
                case SYMBOL:
                    if (paren_count == 0)
                        throw new RuntimeException(String.format("Symbol not inside parenthesis at line %d\n", tok.line));
                    parseTree.addTerminal(tok.value);
                    break;
                case COMMENT:
                    break;
                case UNKNOWN:
                    throw new RuntimeException(String.format("Invalid token '%s' at line %d\n", tok.value, tok.line));
            }

        }

        return parseTree;
    }

}
