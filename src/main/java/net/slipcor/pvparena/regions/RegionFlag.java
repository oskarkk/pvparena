package net.slipcor.pvparena.regions;

/**
 * RegionFlag for tick events
 * <p/>
 * <pre>
 * NOCAMP -   players not moving will be damaged
 * DEATH -    players being here will die
 * WIN -      players being here will win
 * LOSE -     players being here will lose
 * NODAMAGE - players being here will receive no damage
 * </pre>
 */
public enum RegionFlag {
    NOCAMP, DEATH, WIN, LOSE, NODAMAGE
}
