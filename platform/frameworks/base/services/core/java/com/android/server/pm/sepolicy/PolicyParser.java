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
    private static final boolean DEBUG_SEPOLICY = true;

    private static final String UNTRUSTED_APP = "untrusted_app";
    private static final String ISOLATED_APP = "isolated_app";

    private static final String APP_DATA_FILE = "app_data_file";

    private static final String CIL_KEY_BLOCK = "block";
    private static final String CIL_KEY_SELF = "self";
    private static final String CIL_KEY_TYPE = "type";
    private static final String CIL_KEY_TYPEBOUNDS  = "typebounds";
    private static final String CIL_KEY_TYPEATTRIBUTE  = "typeattribute";
    private static final String CIL_KEY_TYPEATTRIBUTESET  = "typeattributeset"; // doesn't support expr
    private static final String CIL_KEY_TYPETRANSITION  = "typetransition";
    private static final String CIL_KEY_CALL  = "call"; // doesn't support input lists with more than 1 argument

    private static final ArrayList<String> CIL_KEY_AVRULES = new ArrayList<>(
            Arrays.asList("allow", "auditallow", "dontaudit", "neverallow", "allowx", "auditallowx",
                    "dontauditx","neverallowx")
    );

    private static final ArrayList<String> CIL_KEY_UNSUPPORTED = new ArrayList<>(
            Arrays.asList(
                    "*", "tunableif", "typechange", "tunable", "role", "user",
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

    private static final ArrayList<String> SEAPP_MACRO_SUPPORTED = new ArrayList<>(
            // list of available seapp macros
      Arrays.asList(
              "md_appdomain", "md_netdomain", "md_bluetoothdomain", "md_untrusteddomain",
              "mt_appdatafile"
      )
    );

    private static final ArrayList<String> SEAPP_MACRO_EXPANDED = new ArrayList<>(
            // list of available seapp macros
            Arrays.asList(
                    "md_appdomain", "md_untrusteddomain"
            )
    );

    public static class ParseTree {

        private NonTerminal root, current;

        private String namespace;
        private ArrayList<String> types;
        private ArrayList<String> typetransition_dest;
        private ArrayList<String> typetransition_src;
        private ArrayList<String> typeattribute;
        private HashMap<String, ArrayList<String>> typebounds;
        private HashMap<String, ArrayList<String>> typeattributeset;
        private HashSet<String> avrule_src;  // to avoid slowing down on duplicates
        private HashSet<String> avrule_tgt;  // to avoid slowing down on duplicates
        private HashMap<String, ArrayList<String>> macros;
        private ArrayList<String> tmpfs_types;
        private ArrayList<String> file_types;
        private ArrayList<String> domain_types;
        private ArrayList<String> bounded_to_untrusted_app, bounded_to_app_data_file;

        public ParseTree() {
            root = new NonTerminal(null);
            current = root;

            namespace = null;
            types = new ArrayList<>();
            typetransition_dest = new ArrayList<>();
            typetransition_src = new ArrayList<>();
            typeattribute = new ArrayList<>();
            typebounds = new HashMap<>();
            typeattributeset = new HashMap<>();
            avrule_src = new HashSet<>();
            avrule_tgt = new HashSet<>();
            macros = new HashMap<>();
            tmpfs_types = new ArrayList<>();
            file_types = new ArrayList<>();
            domain_types = new ArrayList<>();
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

            // check the policy to be wrapped in a block statement
            if(!blockWrapped())
                return false;

            // parse the content of the sepolicy file
            if(!treeWalk(root))
                return false;

            // print the statements listed in the sepolicy file
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
                for (String dest : typetransition_dest)
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
                Slog.d(TAG, "TYPES USED IN CALL TO MACROS");
                for (String macro : macros.keySet()) {
                    StringBuilder sb = new StringBuilder(macro);
                    sb.append(": ");
                    ArrayList<String> set = macros.get(macro);
                    for (String type : set) {
                        sb.append(type);
                        sb.append(", ");
                    }
                    sb.delete(sb.lastIndexOf(","), sb.length());
                    Slog.d(TAG, sb.toString());
                }
            }

            /**
             *  The following ensures the policy to be compliant to the SEApp restrictions.
             *  Please note that the policy will also be compiled by secilc, so malformed policies
             *  will be automatically discarded.
             **/

            // the policy has to be wrapped in a namespace equal to the app package name
            // (the . is replaced with _)
            if (!namespace.equals(pkgName.replace(".","_"))) {
                if (DEBUG_SEPOLICY)
                    Slog.d(TAG, "SEApp PolicyParser error: block name not compliant to pkg name");
                return false;
            }

            // types have to be different to untrusted_app, app_data_file and any other type-attribute.
            // The the global (i.e., the platform's) untrusted_app and app_data_file will be used
            // to bound the types defined locally
            for (String type : types)
                if (type.equals(UNTRUSTED_APP) || type.equals(APP_DATA_FILE)
                        || typeattribute.contains(type)) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: defined a type equal to one among:" +
                            " untrusted_app, app_data_file or a type-attribute (" + type + ")");
                    return false;
                }

            // type-attributes have to be different to untrusted_app and to app_data_file
            // (N.B. it may not be required since type-attributes cannot take part in a typebound stmt)
            for (String type : typeattribute)
                if (type.equals(UNTRUSTED_APP) || type.equals(APP_DATA_FILE)) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: defined a type-attribute equal to" +
                                " one among: untrusted_app or app_data_file (" + type + ")");
                    return false;
                }

            // get all local types bounded to untrusted_app
            ArrayList<String> queue = new ArrayList<>(Arrays.asList(UNTRUSTED_APP));
            bounded_to_untrusted_app = new ArrayList<>();
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

            // get all local types bounded to app_data_file
            queue = new ArrayList<>(Arrays.asList(APP_DATA_FILE));
            bounded_to_app_data_file = new ArrayList<>();
            while (!queue.isEmpty()) {
                String vertex = queue.get(0);
                ArrayList<String> neighbors = typebounds.get(vertex);
                if (neighbors != null) {
                    for (String neighbor : neighbors) {
                        if (!bounded_to_app_data_file.contains(neighbor)) {
                            bounded_to_app_data_file.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                queue.remove(0);
            }

            // all local types have to be bounded to one between app_data_file and untrusted_app
            for (String type : types)
                if (! (bounded_to_app_data_file.contains(type) || bounded_to_untrusted_app.contains(type))){
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + type + ") is an unbounded" +
                                " local type");
                    return false;
                }
            // empty intersection
            for (String type : bounded_to_app_data_file)
                if (bounded_to_untrusted_app.contains(type)){
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + type + ") bounded to " +
                                " both untrusted_app and app_data_file");
                    return false;
                }

            // union of all bounded types
            ArrayList<String> bounded_union = new ArrayList<>();
            bounded_union.addAll(bounded_to_app_data_file);
            bounded_union.addAll(bounded_to_untrusted_app);

            if (DEBUG_SEPOLICY) {
                Slog.d(TAG, "TYPES BOUNDED TO untrusted_app");
                for (String type : bounded_to_untrusted_app)
                    Slog.d(TAG, type);
                Slog.d(TAG, "TYPES BOUNDED TO app_data_file");
                for (String type : bounded_to_app_data_file)
                    Slog.d(TAG, type);
            }

            // typeattributeset stmt: all elements in the set that relate to the typeattribute
            // (which must be defined locally) must be local and bounded
            // This way, we prevent the platform policy to be modified by implicit propagation of
            // permission
            for (String typeattr : typeattributeset.keySet()) {
                if (!typeattribute.contains(typeattr)) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: the typeattributeset statement" +
                                " relates to a non-local type-attribute(" + typeattr + ")");
                    return false;
                }
                ArrayList<String> list = typeattributeset.get(typeattr);
                for (String type : list) {
                    // the type is local and bounded
                    boolean left_pred = types.contains(type) && bounded_union.contains(type);
                    // type is a local type-attribute that relates to local bounded types
                    boolean right_pred = typeattribute.contains(type) && resolveTypeAttribute(type) != null &&
                            bounded_union.containsAll(resolveTypeAttribute(type));
                    if ( !(left_pred || right_pred) ) {
                        if (DEBUG_SEPOLICY)
                            Slog.d(TAG, "SEApp PolicyParser error: (" + type + "), " +
                                    "associated to type-attribute (" + typeattr + "), is not local" +
                                    " and/or bounded");
                        return false;
                        }
                }
            }

            // all typetransition dest must be local and bounded
            for (String dest : typetransition_dest) {
                boolean left_pred = types.contains(dest) && bounded_union.contains(dest);
                boolean right_pred = typeattribute.contains(dest) && resolveTypeAttribute(dest) != null
                        && bounded_union.containsAll(resolveTypeAttribute(dest));
                if (!( left_pred || right_pred )) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + dest + ") is a non local " +
                                "and/or bounded transition destination type");
                    return false;
                }
            }

            // only typetransition with locally defined source type can be defined
            for (String src_type: typetransition_src){
                boolean left_pred = types.contains(src_type) && bounded_union.contains(src_type);
                boolean right_pred = typeattribute.contains(src_type) && resolveTypeAttribute(src_type) != null
                        && bounded_union.containsAll(resolveTypeAttribute(src_type));
                if (! (left_pred || right_pred)){
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + src_type + ") is a non local " +
                                "and/or bounded source type used in a transition");
                    return false;
                }
            }

            // all avrule_src must be local and bounded (no Allow from system type to local type allowed)
            for (String src : avrule_src) {
                boolean left_pred = types.contains(src) && bounded_union.contains(src);
                boolean right_pred = typeattribute.contains(src) && resolveTypeAttribute(src) != null
                        && bounded_union.containsAll(resolveTypeAttribute(src));
                if (!( left_pred || right_pred )) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + src + ") is a non local " +
                                "and/or bounded source avrule type");
                    return false;
                }
            }

            // all avrule_tgt must not be defined by another package
            for (String tgt : avrule_tgt) {
                if (tgt.contains(".") && !tgt.startsWith(".") && !validGlobalType(tgt)) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + tgt + ") is a non local " +
                                "defined avrule target type");
                    return false;
                }
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
                typetransition_dest.add(dest.data);
                Terminal src = (Terminal) nt.children.get(1);
                typetransition_src.add(src.data);

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
            } else if (key.data.equals(CIL_KEY_CALL)) {

                if (nt.children.size() != 3 || nt.children.get(1) instanceof NonTerminal)
                    return false;
                Terminal macro = (Terminal) nt.children.get(1);
                // check the called macro to be defined
                if (!SEAPP_MACRO_SUPPORTED.contains(macro.data))
                    return false;
                // input parameters must be enclosed in parentheses (it has to be a non-terminal)
                if (nt.children.get(2) instanceof Terminal)
                    return false;
                NonTerminal set = (NonTerminal) nt.children.get(2);
                //only one parameter allowed in the input list (we assume the developer to user is unaware of )
                if (set.children.size() != 1 || set.children.get(0) instanceof NonTerminal)
                    return false;
                Terminal t = (Terminal) set.children.get(0);
                // add the input to the macros HashMap
                if (!macros.containsKey(macro.data)){
                    ArrayList<String> input_types = new ArrayList<>();
                    input_types.add(t.data);
                    macros.put(macro.data, input_types);
                } else {
                    ArrayList<String> prev_list = macros.get(macro.data);
                    prev_list.add(t.data);
                }
            } else if (CIL_KEY_UNSUPPORTED.contains(key.data)) {
                return false;
            }

            return true;
        }

        private boolean postExpansionChecks(){
            // file types have to be bounded to app_data_file
            for (String type : file_types)
                if (bounded_to_untrusted_app.contains(type)) {
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + type + ") is a file type" +
                                " but is bounded to untrusted_app");
                    return false;
                }
            // domain types have to be bounded to untrusted_app
            for (String type : domain_types)
                if (bounded_to_app_data_file.contains(type)){
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + type + ") is a domain type" +
                                " but is bounded to app_data_file");
                    return false;
                }
            // only locally defined types can be used in macros call argument input list
            ArrayList<String> macro_types = new ArrayList<>();
            macro_types.addAll(file_types);
            macro_types.addAll(domain_types);
            for (String type: macro_types)
                if (!types.contains(type)){
                    if (DEBUG_SEPOLICY)
                        Slog.d(TAG, "SEApp PolicyParser error: (" + type + ") is used in the " +
                                "a call to macro argument list but it is not a localtype");
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
                    return null; // avoid cycles
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

        private boolean validGlobalType(String type) {
            String localType = getLocalType(type);
            return (localType != null && types.contains(localType));
        }

        private String getLocalType(String type) {
            if (type != null && type.startsWith(namespace + "."))
                return type.substring(namespace.length() + 1);
            return null;
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

        /**
         * This method expands the parse tree of a VALID sepolicy.cil file, it is not safe to invoke
         * it if the isCompliant method returns false for the current parse tree
         */
        public void expander(){
            // expand the parse tree input argument list and retrieve the custom types to be
            // added to the parse tree
            expanderHelper(this.root);
            // add custom types to the parse tree
            for (String type : tmpfs_types)
                addExpandedType(type);
        }

        private void expanderHelper(Node node){
            if (node == null)
                return;
            NonTerminal nt = (NonTerminal) node;
            if (nt.children.size() != 0 && nt.children.get(0) instanceof Terminal){
                String statement_type = ((Terminal) nt.children.get(0)).data;
                if (statement_type.equals(CIL_KEY_CALL)) {
                    // since we have a compliant macro, it has the expected structure:
                    // (call macro_name (input_type)), no need to further check it
                    String macro = ((Terminal) nt.children.get(1)).data;
                    NonTerminal set;
                    String new_type;
                    // check whether it's one of the macros that require expansion
                    if (SEAPP_MACRO_EXPANDED.contains(macro)) {
                        set = (NonTerminal) nt.children.get(2);
                        String old_type = ((Terminal) set.children.get(0)).data;
                        // get the expanded type to be added to the parse tree
                        new_type = old_type.concat("_tmpfs");
                        // add it to the structure that keeps track of the types to be added
                        tmpfs_types.add(new_type);
                        domain_types.add(old_type);
                        if (DEBUG_SEPOLICY)
                            Slog.d(TAG, "Custom type to be added to the parse tree: " + new_type);
                        // manage the expansion to the macro-call argument list
                        set.children.add(new Terminal(set, new_type));
                    } else if (macro.equals("mt_appdatafile")){
                        set = (NonTerminal) nt.children.get(2);
                        String f_type = ((Terminal) set.children.get(0)).data;
                        file_types.add(f_type);
                    }


                }
            }
            for (Node n : nt.children)
                if (n instanceof NonTerminal)
                    expanderHelper(n);
        }

        private void addExpandedType(String expandedType){
            // go down to the level to which the new type's S-expr declaration is added
            NonTerminal declarations_level = (NonTerminal) this.root.children.get(0);

            // build the custom type non-terminal
            NonTerminal et_declaration = new NonTerminal(declarations_level);
            et_declaration.children.add(new Terminal(et_declaration, CIL_KEY_TYPE));
            et_declaration.children.add(new Terminal(et_declaration, expandedType));

            // nest the non-terminal to the tree (position 2 means: immediately after the block name
            // declaration, before any other policy declaration inside the block)
            declarations_level.children.add(2, et_declaration);
        }

        public void serializer(StringBuilder sb) {
            serializerHelper(sb, this.root);
        }

        private void serializerHelper(StringBuilder sb, Node node) {
            if (node == null)
                return;
            NonTerminal nt = (NonTerminal) node;
            // flags
            boolean closing_Level = false;
            boolean block_level = false;
            if (nt.children.size() != 0 && !(nt.children.get(0) instanceof NonTerminal)) {
                String ctype = ((Terminal) nt.children.get(0)).data;
                if (ctype.equals(CIL_KEY_CALL) ||
                        ctype.equals(CIL_KEY_TYPE) ||
                        CIL_KEY_AVRULES.contains(ctype) ||
                        ctype.equals(CIL_KEY_TYPEBOUNDS) ||
                        ctype.equals(CIL_KEY_TYPEATTRIBUTE) ||
                        ctype.equals(CIL_KEY_TYPETRANSITION) ||
                        ctype.equals(CIL_KEY_TYPEATTRIBUTESET))
                    closing_Level = true;
                else if (ctype.equals(CIL_KEY_BLOCK))
                    block_level = true;
            }
            if (block_level)
                sb.append("(");
            // append non-terminals
            boolean not_first_element = false;
            for (Node n : nt.children) {
                if (n instanceof Terminal) {
                    Terminal current = (Terminal) n;
                    if (!block_level && !not_first_element)
                        if (closing_Level)
                            sb.append("\t(");
                        else
                            sb.append(" (");
                    if (not_first_element)
                        sb.append(" ");
                    sb.append(current.data);
                    not_first_element = true;
                }
                else {
                    if (block_level)
                        sb.append("\n");
                    serializerHelper(sb, n);
                }
            }
            if (block_level)
                sb.append("\n");
            if (node != this.root)
                sb.append(")");
            if (closing_Level || block_level)
                sb.append("\n");
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

    /**
     * This method expands the parse tree. By doing this, we support the propagation of untrusted_app domain
     * permission to types introduced by the developer keeping the knowledge of the platform policy to a minimum.
     * @param parseTree The parse tree to be modified
     */
    public static boolean expandAST(PolicyParser.ParseTree parseTree){
        parseTree.expander();
        return parseTree.postExpansionChecks();
    }

    /**
     * This method produces a string representation of the parse tree
     * @param parseTree The parse tree to be printed
     * @return The string representation of the parse tree
     */
    public static StringBuilder serializeAST(PolicyParser.ParseTree parseTree){
        StringBuilder sb = new StringBuilder(1000);
        parseTree.serializer(sb);
        if (DEBUG_SEPOLICY)
            Slog.d(TAG, "EXPANDED SEPOLICY.CIL: \n" + sb.toString());
        return sb;
    }

}