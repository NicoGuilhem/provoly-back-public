package com.provoly.common.transfo;

public class TransfoErrorMissingNode extends TransfoError {

    private final LinkDto link;

    public TransfoErrorMissingNode(LinkDto link) {
        this.link = link;
    }

    public LinkDto getLink() {
        return link;
    }
}
