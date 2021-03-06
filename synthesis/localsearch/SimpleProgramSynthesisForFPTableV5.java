/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FicPlaySynthesizer.synthesis.localsearch;

import FicPlaySynthesizer.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.utils.SerializableController;
import FicPlaySynthesizer.synthesis.localsearch.searchImplementation.DetailedSearchResult;
import FicPlaySynthesizer.synthesis.localsearch.searchImplementation.SAForFPTableV5;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import util.Pair;
import FicPlaySynthesizer.synthesis.localsearch.Fp_element;

/**
 *
 * @author rubens
 */
public class SimpleProgramSynthesisForFPTableV5 implements LocalSearch {

    //private final String pathTableScripts = System.getProperty("user.dir").concat("/Table/");
    //private ScriptsTable scrTable;
    private SAForFPTableV5 searchAlgorithm;
    private BuilderDSLTreeSingleton builder;
    private String uniqueID = UUID.randomUUID().toString();
    private HashMap<String, Fp_element> fp_group;

    public SimpleProgramSynthesisForFPTableV5(SAForFPTableV5 search) {
        //this.scrTable = new ScriptsTable(pathTableScripts);
        this.searchAlgorithm = search;
        this.builder = BuilderDSLTreeSingleton.getInstance();
        this.fp_group = new HashMap<>();
    }

    public SimpleProgramSynthesisForFPTableV5(SAForFPTableV5 search, List<iDSL> initial_fp_group) {
        this(search);
        for (iDSL ast : initial_fp_group) {
            check_inclusion(ast);
        }
    }

    @Override
    public List performRun() {
        System.out.println("Program ID " + uniqueID);
        System.out.println("Class:" + this.getClass().getSimpleName());
        System.out.println("Class:" + searchAlgorithm.getClass().getSimpleName());
        System.out.println("----------------------------------");
        String path = System.getProperty("user.dir").concat("/logs2/");
        List<Pair<String, Float>> last_best_matchs = new ArrayList<>();
        int count = 1;
        iDSL last_best_iteration = null;
        for (int i = 1; i <= SettingsAlphaDSL.get_number_alphaDSL_iterations(); i++) {

            float best_score = -9999;
            iDSL best_response_ast = null;
            List<Pair<String, Float>> best_matchs = new ArrayList<>();
            for (int j = 0; j < SettingsAlphaDSL.get_qtd_iterations_for_SA(); j++) {

                DetailedSearchResult res;
                if (last_best_iteration != null) {
                    res = callAlgorithm(last_best_iteration);
                } else {
                    res = callAlgorithm(builder.buildS1Grammar());
                }
//                if ((!res.wasDefeat_by_fp_group()) && res.getWinner() > best_score) {
                if (res.getWinner() > best_score) {
                    System.out.println(">>>>>> Iteration " + i + " step " + j
                            + " current best score: " + best_score + " new best score: " + res.getWinner()
                            + "\n New ast: " + res.getsWinner().translate()
                            + "\n>>>>>>>>>>>>>>>>>");
                    best_score = res.getWinner();
                    best_response_ast = (iDSL) res.getsWinner().clone();
                    best_matchs = res.getMatchs();
                }
            }
            if (best_response_ast != null) {

//                if (!best_than_last_ast_included(last_best_matchs, best_matchs)) {
//                    System.out.println("##>>> Solution ignored by non general improvement in qualitative evaluation!"
//                            + "\n#######\n Evaluation " + i
//                            + " \nBest Ast: " + best_response_ast.translate()
//                            + "\n#######\n");
//                } else {
                    SerializableController.saveSerializable(best_response_ast, "dsl_" + uniqueID + "_id_" + count + ".ser", path);
                    count++;
                    System.out.println("@@>>>>>>> Included new AST: " + best_response_ast.translate());
                    check_inclusion((iDSL) best_response_ast);
                    last_best_matchs = best_matchs;
                    if (last_best_iteration != null) {
                        System.out.println("@@>>>>>>> old last best AST: " + last_best_iteration.translate());
                    }
                    last_best_iteration = (iDSL) best_response_ast.clone();
                    System.out.println("@@>>>>>>> new last best AST: " + last_best_iteration.translate());
                    System.out.print("\n#######\n Evaluation " + i
                            + " \nBest Ast: " + best_response_ast.translate()
                            + "\n#######\n");
//                }
            }

            System.gc();
        }
        return new ArrayList(this.fp_group.keySet());
    }

    private DetailedSearchResult callAlgorithm(iDSL sc_improve) {
        return this.searchAlgorithm.run(sc_improve, fp_group.values());
    }

    private void check_inclusion(iDSL idsl) {
        String elem = idsl.translate();
        if (this.fp_group.containsKey(elem)) {
            this.fp_group.get(elem).setCounter(1);
        } else {
            Fp_element newElem = new Fp_element(idsl);
            this.fp_group.put(elem, newElem);
        }
    }

    private boolean best_than_last_ast_included(List<Pair<String, Float>> last_best_matchs, List<Pair<String, Float>> best_matchs) {

        if (last_best_matchs.isEmpty() || (last_best_matchs.size() == best_matchs.size())) {
            return true;
        }
        float last = 0.0f;
        List<String> elements = new ArrayList<>();
        for (Pair<String, Float> t : last_best_matchs) {
            last += t.m_b;
            elements.add(t.m_a);
        }
        float best = 0.0f;
        float dif = 0.0f;
        boolean use_dif = false;
        for (Pair<String, Float> t : best_matchs) {
            if (elements.contains(t.m_a)) {
                best += t.m_b;
            } else {
                dif = t.m_b;
                use_dif = true;
            }
        }
        float dif_enemy = 0;
        if (use_dif) {
            dif_enemy = 4 - dif;
        }
        if ((best + dif) < (last + dif_enemy)) {
            return false;
        }
        return true;
    }

}
