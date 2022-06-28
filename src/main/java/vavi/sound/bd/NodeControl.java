/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.util.ArrayList;
import java.util.List;


class NodeControl {

    protected Node bestNode;

    // Option 1 (also required for 2)
    protected float maxCSN;

    // Option 2
    protected float maxCSNTimeout;

    public NodeControl() {
        maxCSN = -1;
        bestNode = null;
        maxCSNTimeout = 0;
    }

    public void initialize() {
        // Reset list of nets
        nodes.clear();
    }

    public void executeStep(float[] inputBuffer, IOIStatCollector.IOIStats stats) {

        // REMOVE, ADJUST and MARK
        // Compare Dominant IOI list with current timing net periods
        // Remove, adjust period, and reference count as necessary
        for (int i = 0; i < nodes.size();) {
            boolean found = false;
            float idealPeriod = 0;

            // Search dominant list for this loop length
            for (IOIStatCollector.IOIPeriodEntry dom : stats.dominantIOIs) {
                // If found a Node with a similar period (within tolerance)
                // we've found a match, so don't remove this node and clear
                // the similar node period entry from the dominant list
                if (Math.abs(nodes.get(i).period() - dom.period) < Utils.params.nodeMaxDiff) {
                    // Mark found, also find highest csn output
                    found = true;
                    dom.refCount++;
                    if (nodes.get(i).csnOutput() > dom.highestEnergy)
                        dom.highestEnergy = nodes.get(i).csnOutput();

                    // Remember ideal period
                    idealPeriod = dom.period;
                    break;
                }
            }

            if (found) {
                // This node closely exists in the dominant IOI list, adjust the node to be
                // closer to the IOI histogram peak
                nodes.get(i).setIdealPeriod(idealPeriod);
                i++;
            } else if ((nodes.get(i).csnOutput() <= -0.5) && (bestNode != nodes.get(i))) {
                // Remove this Node
                Node tempNode = nodes.get(i);
                i++;

                // Tell Loop to remove itself
                tempNode.csn().flushCSNLinks();
                if (bestNode == tempNode) {
                    bestNode = null;
                }

                nodes.remove(tempNode);
            } else {
                i++;
            }
        }

        // ADD
        // If there is anything left in the dominant IOI list, it should be added
        for (IOIStatCollector.IOIPeriodEntry dom : stats.dominantIOIs) {
            if (dom.refCount == 0) {
                // No references, add a node
//                float period = dom.period;
                addNode(dom.period);
            } else if (dom.refCount > 1) {
                // Debug
//                float P = dom.period;
//                int ref = dom.refCount;
//                float E = dom.highestEnergy;

                // More than one reference, remove all but most energetic node
                for (int i = 0; i < nodes.size();) {
                    if (Math.abs(nodes.get(i).period() - dom.period) < Utils.params.nodeMaxDiff) {
                        // Matching node to dominant IOI, is highest energy?
                        if (nodes.get(i).csnOutput() < dom.highestEnergy) {
                            // No, so remove this weaker node
                            Node tempNode = nodes.get(i);
                            i++;

                            // Tell Loop to remove itself
                            if (bestNode != tempNode) {
                                tempNode.csn().flushCSNLinks();
                                nodes.remove(tempNode);
                            }
                        } else {
                            i++;
                        }
                    } else {
                        i++;
                    }
                }
            }

            // Reset Stats
            dom.refCount = 0;
            dom.highestEnergy = 0;
        }

        // UPDATE
        // Update Loops
        for (Node node : nodes) {
            node.executeStep(inputBuffer);
        }

        // Commit Step - Lock in CSN updated values, find top CSN Output
        Node nodeBestCandidate = null;
        float maxCSN = Utils.params.csnMinAct;

        for (Node node : nodes) {
            // Commit
            node.commitStep();

            float csnOutput = node.csnOutput();

            // Option 2
            // Update statistics   
            if (csnOutput > maxCSN) {
                // A new best is found!
                nodeBestCandidate = node;
                // Max CSN for now is just the CSN value - we want to find the best!
                maxCSN = csnOutput;
            }
            // Testing/Debugging
//            if ((Math.abs(node.period() - 0.27f) < 0.01)) {
//                nodeBestCandidate = node;
//            }
        }

        // Option 2
        if (null != nodeBestCandidate && null == bestNode) {
            bestNode = nodeBestCandidate;
        }

        // Output CSN results to parent
        if ((null != bestNode) && (null != nodeBestCandidate)) {
            if (bestNode != nodeBestCandidate) {
                // Current max has been superceded, increment timeout
                maxCSNTimeout += (float) 1 / Utils.params.onsetSamplingRate;
                // Timeout - new top candidate becomes max loop
                if (maxCSNTimeout > Utils.params.csnOutputTimeThresh) {
                    bestNode.selected = false;
                    bestNode = nodeBestCandidate;
                    maxCSNTimeout = 0;

                    // Count the number of times we choose a new node
                    if (Utils.params.trackPerformance)
                        Utils.params.trackChangeNodeCount++;
                }
            } else {
                // Current loop is still the max, reset timeout
                maxCSNTimeout = 0;
            }
        }

        if (bestNode != null && Utils.params.trackPerformance)
            bestNode.selected = true;
    }

    public Node bestNode() {
        return bestNode;
    }

    public List<Node> nodes = new ArrayList<>();

    // Insert new timing net and create links to all other nets
    protected void addNode(float nodePeriod) {
        Node newNode = new Node();
        // Init
        newNode.initialize(nodePeriod);

        // Create Links
        for (Node node : nodes) {
            // Add link between new Net and existing Nets
            newNode.csn().addCSNLink(node);
            node.csn().addCSNLink(newNode);
        }

        // Add new net eo list
        nodes.add(newNode);
    }
}
