package net.jp.hellparadise.testbridge.network.guis;

public enum GuiEnum {

    NONE, // By default, we won't interact with this
    RESULT_PIPE,
    TEMPLATE_PKG,
    // Always put satellite bus at the end!
    SATELLITE_BUS(6);

    final int additional;

    GuiEnum() {
        this.additional = 0;
    }

    GuiEnum(int additional) {
        this.additional = additional;
    }

    public int begin() {
        return ordinal();
    }

    public int end() {
        return this.ordinal() + this.additional;
    }

}
