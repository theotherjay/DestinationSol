package com.miloshpetrov.sol2.game.planet;

import com.badlogic.gdx.math.Vector2;
import com.miloshpetrov.sol2.common.SolMath;
import com.miloshpetrov.sol2.game.SolGame;

public class PlanetBind {
  private final Planet myPlanet;
  private final Vector2 myRelPos;
  private final float myRelAngle;

  public PlanetBind(Planet planet, Vector2 pos, float angle) {
    myPlanet = planet;
    myRelPos = new Vector2();
    float planetAngle = planet.getAngle();
    SolMath.toRel(pos, myRelPos, planetAngle, planet.getPos());
    myRelAngle = angle - planetAngle;
  }

  public void setDiff(Vector2 diff, Vector2 pos) {
    SolMath.toWorld(diff, myRelPos, myPlanet.getAngle(), myPlanet.getPos());
    diff.sub(pos);
  }

  public float getAngleDiff(float angle) {
    return myPlanet.getAngle() + myRelAngle - angle;
  }

  public static PlanetBind tryBind(SolGame game, Vector2 pos, float angle) {
    Planet np = game.getPlanetMan().getNearestPlanet(pos);
    if (!np.isNearGround(pos)) return null;
    return new PlanetBind(np, pos, angle);
  }
}
