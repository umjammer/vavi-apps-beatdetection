/*
 * https://github.com/robharper/beat-detect
 */

package vavi.sound.bd;

import java.util.List;


class NodeCSN {

    protected List<Node> links;

    protected float csnOutput;

    protected float csnOutputNew;

    protected Node node;

    public NodeCSN(Node node) {
        this.node = node;
    }

    public void initialize() {
        // CSN
        links.clear();

        csnOutput = Utils.params.csnMinAct;
        csnOutputNew = Utils.params.csnMinAct;
    }

    // Add specified net to link list and calculate link weight
    public void addCSNLink(Node node) {
//        if (true) { // node.loopLength() < loopLength())
//            // The loop to add is shorter and therefore can affect this loop's activation
//            // formula = dif = 0.5 - abs((LA / LB mod 1) - 0.5)
//            // weight = (max - min) * ((1 - 2 * dif) ^ alpha) + min
//            float big = Math.max(node.LoopLength(), loopLength());
//            float small = Math.min(node.LoopLength(), loopLength());
//            float dif = 0.5 - Math.abs(Math.mod(big / small, 1) - 0.5);
//            linkInfo.link = (params.csnMaxLink - params.csnMinLink) * Math.pow(1 - 2 * dif, params.csnAlpha) + params.csnMinLink;
//        } else {
//            // The loop to add is longer than this and therefore cannot affect this loop's activation
//            linkInfo.link = 0;
//        }

        links.add(node);
    }

    // Remove specified net from link list
    public void removeCSNLink(Node targetNode) {
        for (Node node : links) {
            if (node == targetNode) {
                // Found the one to remove
                links.remove(node);
                break;
            }
        }
    }

    // Tell all linked nets to remove this net from their lists
    public void flushCSNLinks() {
        for (Node node : links) {
            // Remove this Netlist from all other nets' links
            node.csn().removeCSNLink(this.node);
        }
    }

    public void updateCSN(float netEnergy) {
        // Decay the activation
        csnOutputNew = csnOutput * Utils.params.csnDecay;

        // Calculate the linked weighting contributions
        float csnChange = 0;
        for (Node node : links) {
            // Calculate link strength
            float lcmThem = node.period();
            float lcmUs = this.node.period();

            while (Math.abs(lcmThem - lcmUs) > 0.05) {
                if (lcmThem < lcmUs)
                    lcmThem += node.period();
                else
                    lcmUs += this.node.period();
            }
            float x = lcmUs / this.node.period();

//            float link = (Math.pow(0.93f, x * x) / 0.93f) *
//                           (params.csnMaxLink - params.csnMinLink) + params.csnMinLink;

            //float link = (1.0f / Math.sqrt(x)) * (params.csnMaxLink - params.csnMinLink) + params.csnMinLink;
            float link = (1.0f / x) * (Utils.params.csnMaxLink - Utils.params.csnMinLink)
                         + Utils.params.csnMinLink;

            csnChange += link * node.timingNet().netEnergy();
            //csnChange += link * node.csnOutput();
        }
        // Add the leaky integrator input to the change
        csnChange += netEnergy * Utils.params.csnInputLink;

        // Weight the activation change by the distance between current value and
        // the max or min allowed activation level
        if (csnChange > 0)
            csnChange *= Utils.params.csnMaxAct - csnOutput;
        else
            csnChange *= csnOutput - Utils.params.csnMinAct;

        // Calculate new CSN unit activation
        csnOutputNew = Utils.bound(csnOutputNew + csnChange, Utils.params.csnMinAct, Utils.params.csnMaxAct);
    }

    public void commitCSN() {
        csnOutput = csnOutputNew;
    }

    public final float csnOutput() {
        return csnOutput;
    }
}
