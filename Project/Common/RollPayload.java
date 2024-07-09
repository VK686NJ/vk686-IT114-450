package Project.Common;
//vk686 07/07/2024
public class RollPayload extends Payload {
    private int rollResult;
    private int numberOfDice;
    private int sidesPerDie;

    public int getRollResult() {
        return rollResult;
    }

    public void setRollResult(int rollResult) {
        this.rollResult = rollResult;
    }

    public int getNumberOfDice() {
        return numberOfDice;
    }

    public void setNumberOfDice(int numberOfDice) {
        this.numberOfDice = numberOfDice;
    }

    public int getSidesPerDie() {
        return sidesPerDie;
    }

    public void setSidesPerDie(int sidesPerDie) {
        this.sidesPerDie = sidesPerDie;
    }

    @Override
    public String toString() {
        return String.format("RollPayload[%s] Client id [%s] RollResult: [%s] NumberofDice: [%s] Sides Per Die: [%s]",
                getPayloadType(), getClientId(), getRollResult(), getNumberOfDice(), getSidesPerDie());
    }
}