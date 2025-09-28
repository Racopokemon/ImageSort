package com.github.racopokemon.imagesort;

//Maintains a set of boolean conditions (you set the number in the constructor)
//Update boolean conditions with update
//Every update fires a call to the ConditionCallback that YOU provide. In the callback, we give you one boolean: isAnyTrue is true....... if any of the values is true. 

//Useful if several conditions all trigger the same UI etc

//Because no one likes simplicity, there is now also a single blocker condition (updateBlocker). If it is true, it blocks any of the conditions and were always false. 
public class IsAnyTrue {

    public interface ConditionCallback {
        public void onUpdate(boolean isAnyTrue);
    }

    private ConditionCallback callback;
    private boolean[] conditions;
    private boolean blocked;

    public IsAnyTrue(int numberOfConditions, ConditionCallback callback) {
        conditions = new boolean[numberOfConditions];
        this.callback = callback;
    }

    public void update(int index, boolean value) {
        conditions[index] = value;
        doUpdate();
    }

    //If true, were blocking all possibly true conditions. 
    public void updateBlocker(boolean blocked) {
        this.blocked = blocked;
        if (blocked) {
            callback.onUpdate(false);
        } else {
            doUpdate();
        }
    }

    private void doUpdate() {
        if (blocked) {
            callback.onUpdate(false);
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

    public void ignoreEverythingAndSetValue(boolean isAnyTrue) {
        callback.onUpdate(isAnyTrue);
    }
}
