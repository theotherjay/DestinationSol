package com.miloshpetrov.sol2.game.item;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.miloshpetrov.sol2.*;
import com.miloshpetrov.sol2.common.SolMath;
import com.miloshpetrov.sol2.game.GameCols;
import com.miloshpetrov.sol2.game.gun.GunConfig;
import com.miloshpetrov.sol2.game.gun.GunItem;
import com.miloshpetrov.sol2.game.particle.EffectTypes;
import com.miloshpetrov.sol2.game.projectile.ProjectileConfigs;
import com.miloshpetrov.sol2.game.ship.AbilityCharge;
import com.miloshpetrov.sol2.game.sound.SoundManager;
import com.miloshpetrov.sol2.ui.DebugCollector;

import java.util.*;

public class ItemMan {
  public static final String ITEM_CONFIGS_DIR = Const.CONFIGS_DIR + "items/";
  private final HashMap<String,SolItem> myM;
  private final ArrayList<SolItem> myL;
  public final ProjectileConfigs projConfigs;
  public final TextureAtlas.AtlasRegion moneyIcon;
  public final TextureAtlas.AtlasRegion medMoneyIcon;
  public final TextureAtlas.AtlasRegion bigMoneyIcon;
  public final TextureAtlas.AtlasRegion repairIcon;
  private final EngineItem.Configs myEngineConfigs;
  private final SolItemTypes myTypes;
  private final RepairItem myRepairExample;

  public ItemMan(TextureManager textureManager, SoundManager soundManager, EffectTypes effectTypes, GameCols cols) {
    moneyIcon = textureManager.getTex(TextureManager.ICONS_DIR + "money", null);
    medMoneyIcon = textureManager.getTex(TextureManager.ICONS_DIR + "medMoney", null);
    bigMoneyIcon = textureManager.getTex(TextureManager.ICONS_DIR + "bigMoney", null);
    repairIcon = textureManager.getTex(TextureManager.ICONS_DIR + "repairItem", null);
    myM = new HashMap<String, SolItem>();

    myTypes = new SolItemTypes(soundManager, cols);
    projConfigs = new ProjectileConfigs(textureManager, soundManager, effectTypes, cols);
    myEngineConfigs = EngineItem.Configs.load(soundManager, textureManager, effectTypes, cols);

    Shield.Config.loadConfigs(this, soundManager, textureManager, myTypes);
    Armor.Config.loadConfigs(this, soundManager, textureManager, myTypes);
    AbilityCharge.Config.load(this, textureManager, myTypes);

    ClipConfig.load(this, textureManager, myTypes);
    GunConfig.load(textureManager, this, soundManager, myTypes);

    myRepairExample = new RepairItem(myTypes.repair);
    myM.put(myRepairExample.getCode(), myRepairExample);

    myL = new ArrayList<SolItem>(myM.values());
  }

  public void fillContainer(ItemContainer c, String items) {
    List<ItemConfig> list = parseItems(items);
    for (ItemConfig ic : list) {
      for (int i = 0; i < ic.amt; i++) {
        if (SolMath.test(ic.chance)) {
          SolItem item = SolMath.elemRnd(ic.examples).copy();
          c.add(item);
        }
      }
    }
  }

  public List<ItemConfig> parseItems(String items) {
    ArrayList<ItemConfig> res = new ArrayList<ItemConfig>();
    if (items.isEmpty()) return res;
    for (String rec : items.split(" ")) {
      String[] parts = rec.split(":");
      if (parts.length == 0) continue;
      String[] names = parts[0].split("\\|");
      ArrayList<SolItem> examples = new ArrayList<SolItem>();
      for (String name : names) {
        SolItem example = getExample(name.trim());
        if (example == null) {
          throw new AssertionError("unknown item " + name + "@" + parts[0] + "@" + rec + "@" + items);
        }
        examples.add(example);
      }
      if (examples.isEmpty()) throw new AssertionError("no item specified @ " + parts[0] + "@" + rec + "@" + items);

      float chance = 1;
      if (parts.length > 1) {
        chance = Float.parseFloat(parts[1]);
        if (chance <= 0 || 1 < chance) throw new AssertionError(chance);
      }

      int amt = 1;
      if (parts.length > 2) {
        amt = Integer.parseInt(parts[2]);
      }
      ItemConfig ic = new ItemConfig(examples, amt, chance);
      res.add(ic);
    }
    return res;
  }

  public SolItem getExample(String code) {
    return myM.get(code);
  }

  public SolItem random() {
    return myL.get(SolMath.intRnd(myM.size())).copy();
  }

  public void registerItem(SolItem example) {
    String code = example.getCode();
    SolItem existing = getExample(code);
    if (existing != null) {
      throw new AssertionError("2 item types registered for item code " + code + ":\n" + existing + " and " + example);
    }
    myM.put(code, example);
  }

  public EngineItem.Configs getEngineConfigs() {
    return myEngineConfigs;
  }

  public void printGuns() {
    if (true) return;
    ArrayList<GunConfig> l = new ArrayList<GunConfig>();
    for (SolItem i : myM.values()) {
      if (!(i instanceof GunItem)) continue;
      GunItem g = (GunItem) i;
      l.add(g.config);
    }
    Comparator<GunConfig> comp = new Comparator<GunConfig>() {
      public int compare(GunConfig o1, GunConfig o2) {
        return Float.compare(o1.meanDps, o2.meanDps);
      }
    };
    Collections.sort(l, comp);
    StringBuilder sb = new StringBuilder();
    for (GunConfig c : l) {
      sb.append(c.tex.name).append(": ").append(SolMath.nice(c.meanDps)).append("\n");
    }
    String msg = sb.toString();
    DebugCollector.warn(msg);
  }

  public MoneyItem moneyItem(float amt) {
    SolItemType t;
    if (amt == MoneyItem.BIG_AMT) {
      t = myTypes.bigMoney;
    } else if (amt == MoneyItem.MED_AMT) {
      t = myTypes.medMoney;
    } else {
      t = myTypes.money;
    }
    return new MoneyItem(amt, t);
  }

  public RepairItem getRepairExample() {
    return myRepairExample;
  }

  public void addAllGuns(ItemContainer ic) {
    for (SolItem i : myM.values()) {
      if (i instanceof ClipItem && !((ClipItem) i).getConfig().infinite) {
        for (int j = 0; j < 8; j++) {
          ic.add(i.copy());
        }
      }
    }
    for (SolItem i : myM.values()) {
      if (i instanceof GunItem) {
        if (ic.canAdd(i)) ic.add(i.copy());
      }
    }
  }

  public List<MoneyItem> moneyToItems(float amt) {
    ArrayList<MoneyItem> res = new ArrayList<MoneyItem>();
    while (amt > MoneyItem.AMT) {
      MoneyItem example;
      if (amt > MoneyItem.BIG_AMT) {
        example = moneyItem(MoneyItem.BIG_AMT);
        amt -= MoneyItem.BIG_AMT;
      } else if (amt > MoneyItem.MED_AMT) {
        example = moneyItem(MoneyItem.MED_AMT);
        amt -= MoneyItem.MED_AMT;
      } else {
        example = moneyItem(MoneyItem.AMT);
        amt -= MoneyItem.AMT;
      }
      res.add(example.copy());
    }
    return res;
  }
}
