package com.github.racopokemon.imagesort;

//Maintains a set of boolean conditions (you set the number in the constructor)
//Update boolean conditions with update
//Every update fires a call to the ConditionCallback that YOU provide. In the callback, we give you one boolean: isAnyTrue is true....... if any of the values is true. 

//Useful if several conditions all trigger the same UI etc
public class IsAnyTrue {

    public interface ConditionCallback {
        public void onUpdate(boolean isAnyTrue);
    }

    private ConditionCallback callback;
    private boolean[] conditions;

    public IsAnyTrue(int numberOfConditions, ConditionCallback callback) {
        conditions = new boolean[numberOfConditions];
        this.callback = callback;
    }

    public void update(int index, boolean value) {
        conditions[index] = value;
        if (value) {
            callback.onUpdate(true);
        } else {
            for (boolean b : conditions) {
                if (b) {
                    callback.onUpdate(true);
                    return;
                }
            }
            callback.onUpdate(false);
        }
    }
}
