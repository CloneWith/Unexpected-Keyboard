package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** A list of preferences where the users can add items to the end and modify
    and remove items. Backed by a string list. Implement user selection in
    [select()]. */
public abstract class ListGroupPreference extends PreferenceGroup
{
  boolean _attached = false;
  List<String> _values;

  public ListGroupPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOrderingAsAdded(true);
    setLayoutResource(R.layout.pref_listgroup_group);
    _values = new ArrayList<String>();
  }

  /** Overrideable */

  /** The label to display on the item for a given value. */
  String label_of_value(String value, int i)
  {
    return value;
  }

  /** Called when an item is added or modified. Returns [null] to cancel the
      action. */
  abstract void select(SelectionCallback callback);

  /** Load/save utils */

  /** Read a value saved by preference from a [SharedPreferences] object.
      Returns [null] on error. */
  static List<String> load_from_preferences(String key,
      SharedPreferences prefs, List<String> def)
  {
    String s = prefs.getString(key, null);
    return (s != null) ? load_from_string(s) : def;
  }

  /** Decode a list of string previously encoded with [save_to_string]. Returns
      [null] on error. */
  static List<String> load_from_string(String inp)
  {
    try
    {
      List<String> l = new ArrayList<String>();
      JSONArray arr = new JSONArray(inp);
      for (int i = 0; i < arr.length(); i++)
        l.add(arr.getString(i));
      return l;
    }
    catch (JSONException e)
    {
      return null;
    }
  }

  /** Encode a list of string so it can be passed to
      [Preference.persistString()]. Decode with [load_from_string]. */
  static String save_to_string(List<String> l)
  {
    return (new JSONArray(l)).toString();
  }

  /** Protected API */

  /** Set the values. If [persist] is [true], persist into the store. */
  void set_values(List<String> vs, boolean persist)
  {
    _values = vs;
    reattach();
    if (persist)
      persistString(save_to_string(vs));
  }

  void add_item(String v)
  {
    _values.add(v);
    set_values(_values, true);
  }

  void remove_item(String v)
  {
    _values.remove(v);
    set_values(_values, true);
  }

  /** Internal */

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
  {
    String input = (restoreValue) ? getPersistedString(null) : (String)defaultValue;
    if (input != null)
    {
      List<String> values = load_from_string(input);
      if (values != null)
        set_values(values, false);
    }
  }

  @Override
  protected void onAttachedToActivity()
  {
    super.onAttachedToActivity();
    if (_attached)
      return;
    _attached = true;
    reattach();
  }

  void reattach()
  {
    removeAll();
    int i = 0;
    for (String v : _values)
    {
      Item item = this.new Item(getContext(), v);
      item.setTitle(label_of_value(v, i));
      addPreference(item);
      i++;
    }
    addPreference(this.new AddButton(getContext()));
  }

  class Item extends Preference
  {
    final String _value;

    public Item(Context ctx, String value)
    {
      super(ctx);
      _value = value;
      setPersistent(false);
      setWidgetLayoutResource(R.layout.pref_listgroup_item_widget);
    }

    @Override
    protected View onCreateView(ViewGroup parent)
    {
      View v = super.onCreateView(parent);
      v.findViewById(R.id.pref_listgroup_remove_btn)
        .setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View _v)
          {
            remove_item(_value);
          }
        });
      return v;
    }
  }

  final class AddButton extends Preference
  {
    public AddButton(Context ctx)
    {
      super(ctx);
      setPersistent(false);
      setLayoutResource(R.layout.pref_listgroup_add_btn);
    }

    @Override
    protected void onClick()
    {
      select(new SelectionCallback() {
        public void select(String value)
        {
          add_item(value);
        }
      });
    }
  }

  public interface SelectionCallback
  {
    public void select(String value);
  }
}
