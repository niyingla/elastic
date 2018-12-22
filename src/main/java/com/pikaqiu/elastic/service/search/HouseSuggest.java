package com.pikaqiu.elastic.service.search;

/**
 * Created by 瓦力.
 */
public class HouseSuggest {
    private String input;
    /**
     * 默认权重
     */
    private int weight = 10;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
