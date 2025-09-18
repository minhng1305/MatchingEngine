package com.project.matchingengine.models.order;

public enum Stock {
    AAPL("Apple Inc.", 82),
    GOOGL("Alphabet Inc.", 75),
    MSFT("Microsoft Corporation", 86),
    AMZN("Amazon.com, Inc.", 68),
    TSLA("Tesla, Inc.", 77),
    META("Meta Platforms, Inc.", 64),
    NFLX("Netflix, Inc.", 58),
    NVDA("NVIDIA Corporation", 71),
    AMD("Advanced Micro Devices, Inc.", 69),
    INTC("Intel Corporation", 72),
    IBM("International Business Machines Corporation", 74),
    ORCL("Oracle Corporation", 65),
    CSCO("Cisco Systems, Inc.", 78),
    SAP("SAP SE", 81),
    ADOBE("Adobe Inc.", 73),
    CRM("Salesforce, Inc.", 79),
    TWTR("Twitter, Inc.", 62),
    SNAP("Snap Inc.", 56),
    BABA("Alibaba Group Holding Limited", 60),
    TCEHY("Tencent Holdings Limited", 61);

    private final String companyName;
    private final int esgScore;

    Stock(String companyName, int esgScore) {
        this.companyName = companyName;
        this.esgScore = esgScore;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getEsgScore() {
        return esgScore;
    }
}
