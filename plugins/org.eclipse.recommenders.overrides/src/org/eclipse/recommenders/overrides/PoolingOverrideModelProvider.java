package org.eclipse.recommenders.overrides;

import static com.google.common.base.Optional.absent;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.recommenders.models.IBasedName;
import org.eclipse.recommenders.models.IModelRepository;
import org.eclipse.recommenders.models.PoolingModelProvider;
import org.eclipse.recommenders.utils.Constants;
import org.eclipse.recommenders.utils.Zips;
import org.eclipse.recommenders.utils.gson.GsonUtil;
import org.eclipse.recommenders.utils.names.ITypeName;

import com.google.common.base.Optional;
import com.google.gson.reflect.TypeToken;

public class PoolingOverrideModelProvider extends PoolingModelProvider<IBasedName<ITypeName>, IOverrideModel> {

    public PoolingOverrideModelProvider(IModelRepository repository) {
        super(repository, Constants.CLASS_OVRD_MODEL);
    }

    @Override
    protected Optional<IOverrideModel> loadModel(ZipFile zip, IBasedName<ITypeName> key) throws Exception {

        ITypeName type = key.getName();
        String path = Zips.path(type, ".json");
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return absent();
        }

        InputStream is = zip.getInputStream(entry);

        final Type listType = new TypeToken<List<OverrideObservation>>() {
        }.getType();
        final List<OverrideObservation> observations = GsonUtil.deserialize(is, listType);
        if (observations.size() == 0) {
            // XXX sanitize bad models!
            // we still need to ensure minimum quality for models .
            observations.add(new OverrideObservation());
        }
        final JayesOverrideModelBuilder b = new JayesOverrideModelBuilder(type, observations);
        b.createPatternsNode();
        b.createMethodNodes();
        final IOverrideModel network = b.build();
        return Optional.of(network);

    }
}
