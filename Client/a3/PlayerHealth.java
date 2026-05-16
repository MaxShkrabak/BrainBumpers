package a3;

public class PlayerHealth {
    private int currentHP;
    private int maxHP;
    private boolean isDead = false;

    private static final int DEFAULT_MAX_HP = 100;
    private static final int NPC_DAMAGE = 10;

    public PlayerHealth() {
        this.maxHP = DEFAULT_MAX_HP;
        this.currentHP = DEFAULT_MAX_HP;
    }

    public void takeDamage() {
        if (isDead) return;
        currentHP = Math.max(0, currentHP - NPC_DAMAGE);
        if (currentHP <= 0) {
            isDead = true;
        }
    }

    public void heal(int amount) {
        if (isDead) return;
        currentHP = Math.min(maxHP, currentHP + amount);
    }

    public void reset() {
        currentHP = maxHP;
        isDead = false;
    }

    public int getCurrentHP()  { return currentHP; }
    public int getMaxHP()      { return maxHP; }
    public boolean isDead()    { return isDead; }
    public float getPercent()  { return (float) currentHP / maxHP; }
}
