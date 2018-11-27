package com.turvo.objectpool;

import com.turvo.objectpool.Calculator;

public class CalculatorPool extends ObjectPoolService<Calculator> {
    
    public CalculatorPool(int minIdle, int maxIdle) {
		super(minIdle, maxIdle);
	}

	@Override
	protected Calculator createObject() {
        return new Calculator(created() + 1);
    }
}