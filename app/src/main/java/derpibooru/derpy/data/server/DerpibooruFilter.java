package derpibooru.derpy.data.server;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.List;

public class DerpibooruFilter implements Parcelable {
    private final int mId;
    private final String mName;
    private final List<Integer> mHiddenTags;
    private final List<Integer> mSpoileredTags;

    private String mDescription;
    private List<String> mHiddenTagNames = new ArrayList<>();
    private List<String> mSpoileredTagNames = new ArrayList<>();
    private boolean mSystemFilter;
    private int mUserCount;

    public DerpibooruFilter(int filterId, String filterName,
                            List<Integer> hiddenTags, List<Integer> spoileredTags) {
        mId = filterId;
        mName = filterName;
        mHiddenTags = hiddenTags;
        mSpoileredTags = spoileredTags;
    }

    public DerpibooruFilter(int filterId, String filterName,
                            List<Integer> hiddenTags, List<Integer> spoileredTags,
                            String description, List<String> hiddenTagNames,
                            List<String> spoileredTagNames, boolean isSystemFilter,
                            int userCount) {
        this(filterId, filterName, hiddenTags, spoileredTags);

        mDescription = description;
        mHiddenTagNames = hiddenTagNames;
        mSpoileredTagNames = spoileredTagNames;
        mSystemFilter = isSystemFilter;
        mUserCount = userCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DerpibooruFilter) {
            return (((DerpibooruFilter) o).getId() == this.getId());
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getName(), getSpoileredTags());
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public List<Integer> getHiddenTags() {
        return mHiddenTags;
    }

    public List<Integer> getSpoileredTags() {
        return mSpoileredTags;
    }

    public String getDescription() {
        return mDescription;
    }

    public List<String> getHiddenTagNames() {
        return mHiddenTagNames;
    }

    public List<String> getSpoileredTagNames() {
        return mSpoileredTagNames;
    }

    public boolean isSystemFilter() {
        return mSystemFilter;
    }

    public int getUserCount() {
        return mUserCount;
    }

    protected DerpibooruFilter(Parcel in) {
        mId = in.readInt();
        mName = in.readString();

        int[] hiddenTags = in.createIntArray();
        mHiddenTags = Ints.asList(hiddenTags);

        int[] spoileredTags = in.createIntArray();
        mSpoileredTags = Ints.asList(spoileredTags);

        mDescription = in.readString();
        in.readStringList(mHiddenTagNames);
        in.readStringList(mSpoileredTagNames);
        mSystemFilter = (in.readInt() == 1);
        mUserCount = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mName);
        dest.writeIntArray(Ints.toArray(mHiddenTags));
        dest.writeIntArray(Ints.toArray(mSpoileredTags));
        dest.writeString(mDescription);
        dest.writeStringList(mHiddenTagNames);
        dest.writeStringList(mSpoileredTagNames);
        dest.writeInt(mSystemFilter ? 1 : 0);
        dest.writeInt(mUserCount);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DerpibooruFilter> CREATOR = new Parcelable.Creator<DerpibooruFilter>() {
        @Override
        public DerpibooruFilter createFromParcel(Parcel in) {
            return new DerpibooruFilter(in);
        }

        @Override
        public DerpibooruFilter[] newArray(int size) {
            return new DerpibooruFilter[size];
        }
    };
}
